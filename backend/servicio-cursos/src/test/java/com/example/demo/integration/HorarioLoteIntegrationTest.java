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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Publicar un turno regenerado: varias secciones se guardan juntas sin chocar con su horario anterior. */
@Testcontainers
@SpringBootTest(properties = {"jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres", "app.seed-demo-data=false"})
@AutoConfigureMockMvc
class HorarioLoteIntegrationTest {
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
    @Autowired HorarioRepository horarios;
    @Autowired AsignacionAcademicaRepository asignaciones;
    @Autowired SeccionRepository secciones;
    @Autowired CursoRepository cursos;
    @Autowired DocenteRepository docentes;
    @Autowired GradoRepository grados;
    @Autowired NivelRepository niveles;
    @Autowired AulaRepository aulas;

    private Long seccionA;
    private Long seccionB;
    private Long curso;
    private Long docente;
    private Long aula1;
    private Long aula2;

    @BeforeEach
    void preparar() {
        horarios.deleteAll();
        asignaciones.deleteAll();
        secciones.deleteAll();
        cursos.deleteAll();
        docentes.deleteAll();
        grados.deleteAll();
        niveles.deleteAll();
        aulas.deleteAll();
        Nivel nivel = new Nivel(); nivel.setNombre("SECUNDARIA"); nivel.setActivo(true); nivel = niveles.save(nivel);
        Grado grado = new Grado(); grado.setNombre("1ro"); grado.setNivel(nivel); grado.setActivo(true); grado = grados.save(grado);
        seccionA = nuevaSeccion(grado, "A");
        seccionB = nuevaSeccion(grado, "B");
        Curso c = new Curso(); c.setCodigo("MAT-L"); c.setNombre("Matemática"); c.setGradoId(grado.getId()); c.setEstado("ACTIVO");
        curso = cursos.save(c).getId();
        Docente d = new Docente(); d.setNombre("Ana"); d.setApellido("Quispe"); d.setDni("87654321"); d.setEmail("ana.lote@example.test");
        d.setEstado("ACTIVO"); d.setHorasSemanalesContratadas(30);
        docente = docentes.save(d).getId();
        aula1 = nuevaAula("101");
        aula2 = nuevaAula("102");
        for (Long seccion : List.of(seccionA, seccionB)) {
            AsignacionAcademica a = new AsignacionAcademica();
            a.setCurso(cursos.getReferenceById(curso)); a.setDocente(docentes.getReferenceById(docente));
            a.setSeccion(secciones.getReferenceById(seccion)); a.setHorasSemanales(2); a.setAnioEscolar(2026); a.setActiva(true);
            asignaciones.save(a);
        }
        // Horario anterior: Ana en A a las 07:30 y en B a las 08:15.
        guardarAnterior(seccionA, aula1, "07:30", "08:15");
        guardarAnterior(seccionB, aula2, "08:15", "09:00");
    }

    @Test
    void publicaVariasSeccionesIntercambiandoHorasSinChocarConElHorarioAnterior() throws Exception {
        String lote = objectMapper.writeValueAsString(List.of(
                Map.of("seccionId", seccionA, "bloques", List.of(bloque(seccionA, aula1, "08:15", "09:00"))),
                Map.of("seccionId", seccionB, "bloques", List.of(bloque(seccionB, aula2, "07:30", "08:15")))));

        mvc.perform(put("/api/horarios/lote").with(coordinacion())
                        .contentType(MediaType.APPLICATION_JSON).content(lote))
                .andExpect(status().isOk());

        assertThat(horarios.findBySeccionId(seccionA)).singleElement()
                .satisfies(h -> assertThat(h.getHoraInicio()).isEqualTo(LocalTime.of(8, 15)));
        assertThat(horarios.findBySeccionId(seccionB)).singleElement()
                .satisfies(h -> assertThat(h.getHoraInicio()).isEqualTo(LocalTime.of(7, 30)));
    }

    @Test
    void rechazaElLoteSiUnDocenteQuedaEnDosSeccionesALaVezYNoGuardaNada() throws Exception {
        String lote = objectMapper.writeValueAsString(List.of(
                Map.of("seccionId", seccionA, "bloques", List.of(bloque(seccionA, aula1, "10:00", "10:45"))),
                Map.of("seccionId", seccionB, "bloques", List.of(bloque(seccionB, aula2, "10:00", "10:45")))));

        mvc.perform(put("/api/horarios/lote").with(coordinacion())
                        .contentType(MediaType.APPLICATION_JSON).content(lote))
                .andExpect(status().isBadRequest());

        assertThat(horarios.findBySeccionId(seccionA)).singleElement()
                .satisfies(h -> assertThat(h.getHoraInicio()).isEqualTo(LocalTime.of(7, 30)));
    }

    @Test
    void aceptaPeriodosDeCincuentaMinutosDentroDeLaCargaSemanal() throws Exception {
        String lote = objectMapper.writeValueAsString(List.of(
                Map.of("seccionId", seccionA, "bloques", List.of(
                        bloque(seccionA, aula1, "10:00", "10:50"), bloque(seccionA, aula1, "10:50", "11:40")))));
        mvc.perform(put("/api/horarios/lote").with(coordinacion())
                        .contentType(MediaType.APPLICATION_JSON).content(lote))
                .andExpect(status().isOk());
    }

    private Map<String, Object> bloque(Long seccion, Long aula, String inicio, String fin) {
        return Map.of("diaSemana", "LUNES", "horaInicio", inicio + ":00", "horaFin", fin + ":00", "turno", "MANANA",
                "aulaId", aula, "seccionId", seccion, "cursoId", curso, "docenteId", docente);
    }

    private void guardarAnterior(Long seccion, Long aula, String inicio, String fin) {
        Horario h = new Horario();
        h.setDiaSemana("LUNES");
        h.setHoraInicio(LocalTime.parse(inicio));
        h.setHoraFin(LocalTime.parse(fin));
        h.setTurno("MANANA");
        h.setAulaId(aula);
        h.setSeccionId(seccion);
        h.setCurso(cursos.getReferenceById(curso));
        h.setDocente(docentes.getReferenceById(docente));
        horarios.save(h);
    }

    private Long nuevaSeccion(Grado grado, String nombre) {
        Seccion s = new Seccion(); s.setNombre(nombre); s.setGrado(grado); s.setTurno("MANANA"); s.setCapacidadMaxima(30);
        s.setActiva(true); s.setAnioEscolar(2026);
        return secciones.save(s).getId();
    }

    private Long nuevaAula(String nombre) {
        Aula a = new Aula(); a.setNombre(nombre); a.setPabellon("A"); a.setCapacidadMaxima(35); a.setActiva(true); a.setTurnoDisponible("AMBOS");
        return aulas.save(a).getId();
    }

    private RequestPostProcessor coordinacion() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_DIRECTOR"));
    }
}
