package com.example.demo.integration;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {"jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres", "app.seed-demo-data=false"})
@AutoConfigureMockMvc
class AsignacionAcademicaIntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AsignacionAcademicaRepository asignaciones;
    @Autowired SeccionRepository secciones;
    @Autowired CursoRepository cursos;
    @Autowired DocenteRepository docentes;
    @Autowired GradoRepository grados;
    @Autowired NivelRepository niveles;

    private Long seccionId;
    private Long cursoId;
    private Long docenteId;

    @BeforeEach
    void prepararCatalogo() {
        asignaciones.deleteAll();
        secciones.deleteAll();
        cursos.deleteAll();
        docentes.deleteAll();
        grados.deleteAll();
        niveles.deleteAll();
        Nivel nivel = new Nivel(); nivel.setNombre("SECUNDARIA"); nivel.setActivo(true); nivel = niveles.save(nivel);
        Grado grado = new Grado(); grado.setNombre("1ro"); grado.setNivel(nivel); grado.setActivo(true); grado = grados.save(grado);
        Seccion seccion = new Seccion(); seccion.setNombre("A"); seccion.setGrado(grado); seccion.setTurno("MANANA"); seccion.setCapacidadMaxima(30); seccion.setActiva(true); seccion.setAnioEscolar(2026); seccion = secciones.save(seccion);
        Curso curso = new Curso(); curso.setCodigo("MAT-01"); curso.setNombre("Matemática"); curso.setGradoId(grado.getId()); curso.setEstado("ACTIVO"); curso = cursos.save(curso);
        Docente docente = new Docente(); docente.setNombre("Ana"); docente.setApellido("Quispe"); docente.setDni("12345678"); docente.setEmail("ana@example.test"); docente.setEstado("ACTIVO"); docente.setHorasSemanalesContratadas(20); docente = docentes.save(docente);
        seccionId = seccion.getId(); cursoId = curso.getId(); docenteId = docente.getId();
    }

    @Test
    void coordinadorPuedeCrearCargaFormalYDocenteNo() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "cursoId", cursoId, "docenteId", docenteId, "seccionId", seccionId,
                "horasSemanales", 6, "anioEscolar", 2026, "activa", true));
        mvc.perform(post("/api/asignaciones-academicas").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/asignaciones-academicas").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void mantenimientoYDireccionAtiendenTicketsYAuxiliarNo() throws Exception {
        mvc.perform(get("/api/tickets").with(jwt()
                        .jwt(token -> token.claim("rol", "MANTENIMIENTO"))
                        .authorities(new SimpleGrantedAuthority("ROLE_MANTENIMIENTO"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tickets").with(jwt()
                        .jwt(token -> token.claim("rol", "AUXILIAR"))
                        .authorities(new SimpleGrantedAuthority("ROLE_AUXILIAR"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/tickets").with(jwt()
                        .jwt(token -> token.claim("rol", "DIRECTOR"))
                        .authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR"))))
                .andExpect(status().isOk());
        mvc.perform(put("/api/tickets/1/estado").with(jwt()
                        .jwt(token -> token.claim("rol", "DIRECTOR"))
                        .authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PROCESO\"}"))
                .andExpect(status().isNotFound());
    }
}
