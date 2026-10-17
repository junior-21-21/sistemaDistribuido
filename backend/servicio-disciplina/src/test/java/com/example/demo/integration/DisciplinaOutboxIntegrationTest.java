package com.example.demo.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.example.demo.repository.ReporteDisciplinarioRepository;
import com.example.demo.service.AsignacionAcademicaClient;
import com.example.demo.model.ReporteDisciplinario;
import java.util.List;

@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres",
        "app.internal-api-key=clave-interna-de-pruebas",
        "notifications.enabled=true",
        "notifications.outbox-publish-ms=600000",
        "services.notificaciones-url=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
class DisciplinaOutboxIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ReporteDisciplinarioRepository reportes;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AsignacionAcademicaClient asignacionClient;

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM notification_outbox");
        reportes.deleteAll();
    }

    @Test
    void guardaReporteYEventoOutboxEnLaMismaOperacion() throws Exception {
        String body = """
                {"estudianteId":11,"docenteId":4,"seccionId":2,"tipo":"FALTA_LEVE",
                 "descripcion":"Interrupción reiterada durante la clase","observaciones":"Se dialogó con el estudiante"}
                """;

        mvc.perform(post("/api/reportes-disciplinarios")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EMITIDO"))
                .andExpect(jsonPath("$.registradoPor").value("user"));

        Long reporteId = reportes.findAll().get(0).getId();
        Integer eventos = jdbc.queryForObject(
                "SELECT count(*) FROM notification_outbox WHERE evento_id=? AND estado='PENDIENTE'",
                Integer.class, "DISCIPLINA-" + reporteId);
        org.junit.jupiter.api.Assertions.assertEquals(1, eventos);
    }

    @Test
    void rechazaPeticionSinTokenYTipoInvalido() throws Exception {
        String valido = """
                {"estudianteId":1,"docenteId":2,"seccionId":3,"tipo":"MERITO","descripcion":"Apoyo destacado"}
                """;
        mvc.perform(post("/api/reportes-disciplinarios")
                        .contentType(MediaType.APPLICATION_JSON).content(valido))
                .andExpect(status().isUnauthorized());

        String invalido = """
                {"estudianteId":1,"docenteId":2,"seccionId":3,"tipo":"DESCONOCIDO","descripcion":"Dato inválido"}
                """;
        mvc.perform(post("/api/reportes-disciplinarios")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultaPorEstudianteOcultaReportesDeSeccionesAjenas() throws Exception {
        reportes.save(reporte(10L, 1L));
        reportes.save(reporte(10L, 2L));
        when(asignacionClient.seccionesPropias(any(), anyInt())).thenReturn(List.of(1L));

        mvc.perform(get("/api/reportes-disciplinarios/estudiante/10")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].seccionId").value(1));
    }

    private ReporteDisciplinario reporte(Long estudianteId, Long seccionId) {
        ReporteDisciplinario reporte = new ReporteDisciplinario();
        reporte.setEstudianteId(estudianteId); reporte.setSeccionId(seccionId);
        reporte.setDocenteId(5L); reporte.setTipo("MERITO"); reporte.setDescripcion("Registro de prueba");
        reporte.setRegistradoPor("docente1");
        return reporte;
    }
}
