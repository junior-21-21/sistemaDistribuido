package com.example.reportes.integration;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.example.reportes.repository.ActividadRepository;

@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres",
        "app.internal-api-key=clave-interna-de-pruebas",
        "integridad.initial-delay-ms=600000",
        "integridad.interval-ms=600000"
})
@AutoConfigureMockMvc
class ReportesIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ActividadRepository actividades;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        actividades.deleteAll();
    }

    @Test
    void coordinacionRegistraActividadYDocenteNoPuede() throws Exception {
        String body = """
                {"texto":"Horario académico publicado","tipo":"HORARIO","origen":"CURSOS"}
                """;

        mvc.perform(post("/api/actividades")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fecha").exists());

        mvc.perform(post("/api/actividades")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/actividades/recientes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("HORARIO"));
    }

    @Test
    void flywayCreaReconciliacionYLaProtegePorRol() throws Exception {
        Integer versionDos = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version='2' AND success=true",
                Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, versionDos);

        mvc.perform(get("/api/integridad/ejecuciones")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/integridad/ejecuciones")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void canalInternoRegistraActividadSoloConClaveCompartida() throws Exception {
        String body = """
                {"texto":"Papeleta emitida","tipo":"warning","origen":"DISCIPLINA"}
                """;
        mvc.perform(post("/api/internal/actividades")
                        .header("X-Internal-Key", "clave-interna-de-pruebas")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/internal/actividades")
                        .header("X-Internal-Key", "incorrecta")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals(1, actividades.count());
    }
}
