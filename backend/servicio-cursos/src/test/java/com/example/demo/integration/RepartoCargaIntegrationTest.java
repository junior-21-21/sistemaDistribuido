package com.example.demo.integration;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.RepartoCargaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Un curso dictado en varias secciones se reparte entre varios docentes sin sobrecargar a ninguno. */
@Testcontainers
@SpringBootTest(properties = {"jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres", "app.seed-demo-data=false"})
class RepartoCargaIntegrationTest {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired RepartoCargaService reparto;
    @Autowired HorarioRepository horarios;
    @Autowired AsignacionAcademicaRepository asignaciones;
    @Autowired SeccionRepository secciones;
    @Autowired CursoRepository cursos;
    @Autowired DocenteRepository docentes;
    @Autowired GradoRepository grados;
    @Autowired NivelRepository niveles;

    private final List<Long> seccionIds = new ArrayList<>();
    private Long curso;
    private Long ana;
    private Long luis;

    @BeforeEach
    void preparar() {
        horarios.deleteAll();
        asignaciones.deleteAll();
        secciones.deleteAll();
        cursos.deleteAll();
        docentes.deleteAll();
        grados.deleteAll();
        niveles.deleteAll();
        seccionIds.clear();
        Nivel nivel = new Nivel(); nivel.setNombre("SECUNDARIA"); nivel.setActivo(true); nivel = niveles.save(nivel);
        Grado grado = new Grado(); grado.setNombre("PRIMERO"); grado.setNivel(nivel); grado.setActivo(true); grado = grados.save(grado);
        for (String nombre : List.of("A", "B", "C", "D")) {
            Seccion s = new Seccion(); s.setNombre(nombre); s.setGrado(grado); s.setTurno("MANANA"); s.setCapacidadMaxima(30);
            s.setActiva(true); s.setAnioEscolar(2026);
            seccionIds.add(secciones.save(s).getId());
        }
        Curso c = new Curso(); c.setCodigo("MAT-R"); c.setNombre("Matemática"); c.setGradoId(grado.getId()); c.setEstado("ACTIVO");
        curso = cursos.save(c).getId();
        ana = docente("Ana", "11111111", 10);
        luis = docente("Luis", "22222222", 10);
        // Hoy Ana tiene las cuatro secciones: 20 h, el doble de lo contratado.
        for (Long seccion : seccionIds) {
            AsignacionAcademica a = new AsignacionAcademica();
            a.setCurso(cursos.getReferenceById(curso)); a.setDocente(docentes.getReferenceById(ana));
            a.setSeccion(secciones.getReferenceById(seccion)); a.setHorasSemanales(5); a.setAnioEscolar(2026); a.setActiva(true);
            asignaciones.save(a);
        }
    }

    @Test
    void simulaYAplicaUnRepartoEquilibrado() {
        List<RepartoCargaService.Item> items = seccionIds.stream()
                .map(id -> new RepartoCargaService.Item(id, curso, 5)).toList();

        RepartoCargaService.Resultado simulacion = reparto.repartir(
                new RepartoCargaService.Solicitud(2026, List.of(ana, luis), items, true));
        assertThat(simulacion.aplicado()).isFalse();
        assertThat(simulacion.sinAsignar()).isEmpty();
        Map<Long, Long> porDocente = simulacion.plan().stream()
                .collect(Collectors.groupingBy(RepartoCargaService.Linea::docenteId, Collectors.summingLong(RepartoCargaService.Linea::horas)));
        assertThat(porDocente).containsEntry(ana, 10L).containsEntry(luis, 10L);
        assertThat(asignaciones.findAll()).allMatch(a -> a.getDocente().getId().equals(ana));

        RepartoCargaService.Resultado aplicado = reparto.repartir(
                new RepartoCargaService.Solicitud(2026, List.of(ana, luis), items, false));
        assertThat(aplicado.aplicado()).isTrue();
        assertThat(asignaciones.sumarHorasDocente(ana, 2026, null)).isEqualTo(10L);
        assertThat(asignaciones.sumarHorasDocente(luis, 2026, null)).isEqualTo(10L);
    }

    @Test
    void noAplicaNadaSiElEquipoNoAlcanza() {
        List<RepartoCargaService.Item> items = seccionIds.stream()
                .map(id -> new RepartoCargaService.Item(id, curso, 5)).toList();
        RepartoCargaService.Resultado resultado = reparto.repartir(
                new RepartoCargaService.Solicitud(2026, List.of(luis), items, false));
        assertThat(resultado.aplicado()).isFalse();
        assertThat(resultado.sinAsignar()).hasSize(2);
        assertThat(asignaciones.sumarHorasDocente(ana, 2026, null)).isEqualTo(20L);
    }

    private Long docente(String nombre, String dni, int horas) {
        Docente d = new Docente(); d.setNombre(nombre); d.setApellido("Prueba"); d.setDni(dni);
        d.setEmail(nombre.toLowerCase() + "@reparto.test"); d.setEstado("ACTIVO"); d.setHorasSemanalesContratadas(horas);
        return docentes.save(d).getId();
    }
}
