package com.example.demo.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.example.demo.repository.NotaRepository;
import com.example.demo.repository.PonderacionCompetenciaRepository;
import com.example.demo.service.AsignacionAcademicaClient;

@Testcontainers
@SpringBootTest(properties = "jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres")
@AutoConfigureMockMvc
class PonderacionIntegrationTest {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired NotaRepository notas;
    @Autowired PonderacionCompetenciaRepository ponderaciones;
    @MockitoBean AsignacionAcademicaClient asignaciones;

    private final org.springframework.test.web.servlet.request.RequestPostProcessor docente =
            jwt().jwt(token -> token.subject("docente1")).authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"));

    @BeforeEach
    void limpiar() {
        notas.deleteAll();
        ponderaciones.deleteAll();
    }

    private static String pesos(String columnas) {
        return """
                {"seccionId":4,"cursoId":2,"docenteId":3,"bimestre":"I BIMESTRE","periodoAnio":"2026","columnas":[%s]}
                """.formatted(columnas);
    }

    @Test
    void guardaPesosYRechazaSumaMayorA100OColumnasRepetidas() throws Exception {
        mvc.perform(put("/api/notas/ponderaciones").with(docente).contentType(MediaType.APPLICATION_JSON)
                        .content(pesos("""
                                {"competencia":"Tarea 1","peso":30},{"competencia":"Examen","peso":70}""")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columnas.length()").value(2))
                .andExpect(jsonPath("$.columnas[1].competencia").value("Examen"));

        mvc.perform(put("/api/notas/ponderaciones").with(docente).contentType(MediaType.APPLICATION_JSON)
                        .content(pesos("""
                                {"competencia":"Tarea 1","peso":60},{"competencia":"Examen","peso":50}""")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/notas/ponderaciones").with(docente).contentType(MediaType.APPLICATION_JSON)
                        .content(pesos("""
                                {"competencia":"Tarea 1","peso":10},{"competencia":"tarea 1 ","peso":10}""")))
                .andExpect(status().isBadRequest());

        // Se conservan los pesos válidos anteriores
        mvc.perform(get("/api/notas/ponderaciones").with(docente)
                        .param("seccionId", "4").param("cursoId", "2")
                        .param("bimestre", "I BIMESTRE").param("periodoAnio", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columnas[0].peso").value(30.0))
                .andExpect(jsonPath("$.columnas[1].peso").value(70.0));
    }

    @Test
    void eliminarColumnaBorraSusNotasYSuPeso() throws Exception {
        mvc.perform(put("/api/notas/ponderaciones").with(docente).contentType(MediaType.APPLICATION_JSON)
                        .content(pesos("""
                                {"competencia":"Borrador","peso":20},{"competencia":"Examen","peso":80}""")))
                .andExpect(status().isOk());
        String notasJson = """
                [{"escalaLiteral":"A","competencia":"Borrador","bimestre":"I BIMESTRE","periodoAnio":"2026",
                  "estudianteId":1,"cursoId":2,"docenteId":3,"seccionId":4},
                 {"escalaLiteral":"B","competencia":"Examen","bimestre":"I BIMESTRE","periodoAnio":"2026",
                  "estudianteId":1,"cursoId":2,"docenteId":3,"seccionId":4}]
                """;
        mvc.perform(post("/api/notas/bulk").with(docente).contentType(MediaType.APPLICATION_JSON).content(notasJson))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/notas/columna").with(docente)
                        .param("seccionId", "4").param("cursoId", "2").param("docenteId", "3")
                        .param("bimestre", "I BIMESTRE").param("periodoAnio", "2026").param("competencia", "borrador"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notasEliminadas").value(1));

        mvc.perform(get("/api/notas/ponderaciones").with(docente)
                        .param("seccionId", "4").param("cursoId", "2")
                        .param("bimestre", "I BIMESTRE").param("periodoAnio", "2026"))
                .andExpect(jsonPath("$.columnas.length()").value(1))
                .andExpect(jsonPath("$.columnas[0].competencia").value("Examen"));
        org.assertj.core.api.Assertions.assertThat(notas.findAll()).hasSize(1);
    }

    @Test
    void docenteSinCargaNoPuedeCambiarPesosYSupervisorNoEscribe() throws Exception {
        doThrow(new AccessDeniedException("sin carga")).when(asignaciones)
                .validarPropia(any(), eq(2L), eq(3L), anyLong(), any());
        mvc.perform(put("/api/notas/ponderaciones").with(docente).contentType(MediaType.APPLICATION_JSON)
                        .content(pesos("{\"competencia\":\"Examen\",\"peso\":100}")))
                .andExpect(status().isForbidden());

        var director = jwt().jwt(token -> token.subject("director"))
                .authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR"));
        mvc.perform(put("/api/notas/ponderaciones").with(director).contentType(MediaType.APPLICATION_JSON)
                        .content(pesos("{\"competencia\":\"Examen\",\"peso\":100}")))
                .andExpect(status().isForbidden());
    }
}
