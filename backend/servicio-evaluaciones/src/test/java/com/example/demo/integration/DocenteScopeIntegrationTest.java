package com.example.demo.integration;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.example.demo.model.Asistencia;
import com.example.demo.model.Nota;
import com.example.demo.repository.AsistenciaRepository;
import com.example.demo.repository.NotaRepository;
import com.example.demo.service.AsignacionAcademicaClient;
import com.example.demo.service.AsignacionAcademicaClient.AsignacionScope;

@Testcontainers
@SpringBootTest(properties = "jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres")
@AutoConfigureMockMvc
class DocenteScopeIntegrationTest {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired NotaRepository notas;
    @Autowired AsistenciaRepository asistencias;
    @MockitoBean AsignacionAcademicaClient asignaciones;

    @BeforeEach
    void preparar() {
        asistencias.deleteAll();
        notas.deleteAll();
        when(asignaciones.listarPropias(any(), anyInt())).thenReturn(List.of(
                new AsignacionScope(10L, 20L, 30L, 2026)));
        guardarNota(1L, 10L, 20L, 30L);
        guardarNota(2L, 11L, 21L, 31L);
        guardarAsistencia(1L, 10L, 20L, 30L, 100L);
        guardarAsistencia(2L, 11L, 21L, 31L, 101L);
    }

    @Test
    void docenteSoloLeeRegistrosDeSuCarga() throws Exception {
        var docente = jwt().jwt(token -> token.subject("docente1"))
                .authorities(new SimpleGrantedAuthority("ROLE_DOCENTE"));
        mvc.perform(get("/api/notas/page").with(docente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].estudianteId").value(1));
        mvc.perform(get("/api/asistencias/estudiante/1").with(docente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/asistencias/estudiante/2").with(docente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    private void guardarNota(Long estudiante, Long curso, Long docente, Long seccion) {
        Nota item = new Nota();
        item.setEstudianteId(estudiante); item.setCursoId(curso); item.setDocenteId(docente);
        item.setSeccionId(seccion); item.setCompetencia("Competencia"); item.setBimestre("I BIMESTRE");
        item.setPeriodoAnio("2026"); item.setEscalaLiteral("A");
        notas.save(item);
    }

    private void guardarAsistencia(Long estudiante, Long curso, Long docente, Long seccion, Long horario) {
        Asistencia item = new Asistencia();
        item.setEstudianteId(estudiante); item.setCursoId(curso); item.setDocenteId(docente);
        item.setSeccionId(seccion); item.setHorarioId(horario); item.setFecha(LocalDate.of(2026, 9, 23));
        item.setEstado("PRESENTE");
        asistencias.save(item);
    }
}
