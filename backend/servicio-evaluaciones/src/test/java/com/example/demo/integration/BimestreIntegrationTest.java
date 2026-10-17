package com.example.demo.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.example.demo.repository.BimestreAuditoriaRepository;
import com.example.demo.repository.BimestreEstadoRepository;
import com.example.demo.repository.NotaRepository;
import com.example.demo.service.AsignacionAcademicaClient;

@Testcontainers
@SpringBootTest(properties = "jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres")
@AutoConfigureMockMvc
class BimestreIntegrationTest {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired NotaRepository notas;
    @Autowired BimestreAuditoriaRepository auditoria;
    @Autowired BimestreEstadoRepository estados;
    @MockitoBean AsignacionAcademicaClient asignaciones;

    @BeforeEach
    void limpiar() {
        notas.deleteAll();
        auditoria.deleteAll();
        estados.deleteAll();
    }

    @Test
    void cierreBloqueaNotasYReaperturaExigeDirectorYMotivo() throws Exception {
        var subdireccion = jwt().jwt(token -> token.subject("coord.academica"))
                .authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR"));
        var director = jwt().jwt(token -> token.subject("director"))
                .authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR"));
        var docente = jwt().jwt(token -> token.subject("docente1"))
                .authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"));
        String motivoCierre = "Cierre oficial luego de validar las actas";

        mvc.perform(post("/api/bimestres/{anio}/{bimestre}/cerrar", 2026, "I BIMESTRE")
                        .with(subdireccion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"" + motivoCierre + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CERRADO"));

        String nota = """
                {"escalaLiteral":"A","competencia":"Resuelve problemas","bimestre":"I BIMESTRE",
                 "periodoAnio":"2026","estudianteId":1,"cursoId":2,"docenteId":3,"seccionId":4}
                """;
        mvc.perform(post("/api/notas").with(docente)
                        .contentType(MediaType.APPLICATION_JSON).content(nota))
                .andExpect(status().isConflict());

        // Un docente no puede reabrir; Dirección sí, y siempre con motivo
        mvc.perform(post("/api/bimestres/{anio}/{bimestre}/reabrir", 2026, "I BIMESTRE")
                        .with(docente).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Corrección autorizada de actas\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/bimestres/{anio}/{bimestre}/reabrir", 2026, "I BIMESTRE")
                        .with(director).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Corrección autorizada por Dirección\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ABIERTO"));

        mvc.perform(post("/api/notas").with(docente)
                        .contentType(MediaType.APPLICATION_JSON).content(nota))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/bimestres/{anio}/{bimestre}/auditoria", 2026, "I BIMESTRE")
                        .with(director))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }
}
