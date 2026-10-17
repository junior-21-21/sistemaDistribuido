package com.example.demo.integration;

import com.example.demo.repository.AsistenciaRepository;
import com.example.demo.repository.SesionAsistenciaRepository;
import com.example.demo.service.AsignacionAcademicaClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = "jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres")
@AutoConfigureMockMvc
class AsistenciaIntegrationTest {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AsistenciaRepository asistencias;
    @Autowired SesionAsistenciaRepository sesiones;
    @MockitoBean AsignacionAcademicaClient asignacionClient;

    @BeforeEach void limpiar() { asistencias.deleteAll(); sesiones.deleteAll(); }

    @Test
    void guardaBorradorCierraSesionYBloqueaCambios() throws Exception {
        String body = "[{\"fecha\":\"2026-09-12\",\"estado\":\"PRESENTE\",\"estudianteId\":1," +
                "\"horarioId\":10,\"docenteId\":5,\"seccionId\":2,\"cursoId\":3}]";
        var docente = jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"));
        mvc.perform(post("/api/asistencias/bulk").with(docente)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$[0].estado").value("PRESENTE"));

        String sesionJson = mvc.perform(get("/api/asistencias/sesiones").with(docente)
                        .param("horarioId", "10").param("fecha", "2026-09-12"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andReturn().getResponse().getContentAsString();
        JsonNode sesion = objectMapper.readTree(sesionJson);

        mvc.perform(post("/api/asistencias/sesiones/{id}/cerrar", sesion.get("id").asLong()).with(docente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CERRADA"));
        mvc.perform(post("/api/asistencias/bulk").with(docente)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportaAsistenciaPorRangoYCursoParaExportar() throws Exception {
        var docente = jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"));
        for (String[] r : new String[][] {{"2026-09-14", "3"}, {"2026-09-16", "3"}, {"2026-09-16", "4"}, {"2026-10-01", "3"}}) {
            mvc.perform(post("/api/asistencias/bulk").with(docente).contentType(MediaType.APPLICATION_JSON)
                            .content("[{\"fecha\":\"" + r[0] + "\",\"estado\":\"FALTA\",\"estudianteId\":1,\"horarioId\":1" + r[1]
                                    + ",\"docenteId\":5,\"seccionId\":2,\"cursoId\":" + r[1] + "}]"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(get("/api/asistencias/rango").with(docente).param("seccionId", "2")
                        .param("desde", "2026-09-14").param("hasta", "2026-09-18").param("cursoId", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fecha").value("2026-09-14"));
        mvc.perform(get("/api/asistencias/rango").with(docente).param("seccionId", "2")
                        .param("desde", "2026-09-01").param("hasta", "2026-09-30"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/asistencias/rango").with(docente).param("seccionId", "2")
                        .param("desde", "2026-09-30").param("hasta", "2026-09-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aplicaRolesOperativosDiferentesParaNotasYAsistencia() throws Exception {
        var coordinadorAcademico = jwt().authorities(
                new SimpleGrantedAuthority("ROLE_DIRECTOR"));
        var auxiliar = jwt().authorities(new SimpleGrantedAuthority("ROLE_AUXILIAR"));

        mvc.perform(get("/api/notas/page").with(coordinadorAcademico))
                .andExpect(status().isOk());
        mvc.perform(get("/api/notas/page").with(auxiliar))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/asistencias")
                        .param("seccionId", "1").param("fecha", "2026-09-12")
                        .with(auxiliar))
                .andExpect(status().isOk());
    }
}
