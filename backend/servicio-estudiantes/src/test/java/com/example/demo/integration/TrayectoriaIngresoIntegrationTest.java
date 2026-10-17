package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.example.demo.dto.IngresoDTO;
import com.example.demo.model.Estudiante;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.service.AsignacionAcademicaClient;
import com.example.demo.service.IngresoService;
import com.example.demo.service.TrayectoriaService;

/** Ingreso con DNI, traslados y promoción con áreas por recuperar. */
@Testcontainers
@SpringBootTest(properties = {
        "jwt.secret=clave-de-pruebas-con-mas-de-32-caracteres",
        "app.internal-api-key=clave-interna-de-pruebas",
        "app.seed-demo-data=false",
        "app.bootstrap-admin.password=",
        "app.bootstrap-system-admin.password=",
        "notifications.enabled=false"
})
class TrayectoriaIngresoIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired IngresoService ingresos;
    @Autowired TrayectoriaService trayectoria;
    @Autowired EstudianteRepository estudiantes;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AsignacionAcademicaClient asignaciones;

    private Estudiante juan;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM ingresos_colegio");
        jdbc.update("DELETE FROM areas_pendientes");
        jdbc.update("DELETE FROM matriculas");
        jdbc.update("DELETE FROM apoderado_estudiante");
        estudiantes.deleteAll();
        Estudiante e = new Estudiante();
        e.setNombre("Juan");
        e.setApellido("Quispe Mamani");
        e.setDni("73123456");
        e.setGradoId(1L);
        e.setSeccionId(10L);
        e.setEstado("ACTIVO");
        juan = estudiantes.save(e);
    }

    @Test
    void registraElIngresoUnaVezPorDiaLeyendoElCodigoDelDni() {
        // Algunos lectores devuelven el número con el dígito verificador o datos extra.
        IngresoDTO primero = ingresos.registrar("731234561", "ESCANER", "auxiliar1");
        assertThat(primero.estudianteId()).isEqualTo(juan.getId());
        assertThat(primero.yaRegistrado()).isFalse();
        assertThat(primero.estado()).isIn("PUNTUAL", "TARDANZA");
        assertThat(primero.dniFinal()).endsWith("456").doesNotContain("73123");

        IngresoDTO segundo = ingresos.registrar("73123456", "MANUAL", "auxiliar1");
        assertThat(segundo.yaRegistrado()).isTrue();
        assertThat(segundo.id()).isEqualTo(primero.id());
        assertThat(ingresos.resumenDeHoy().ingresaron()).isEqualTo(1);

        assertThatThrownBy(() -> ingresos.registrar("99999999", "ESCANER", "auxiliar1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void promueveConAreaPorRecuperarYLuegoSeSubsana() {
        int anio = TrayectoriaService.anioActual();
        var resumen = trayectoria.promover(new TrayectoriaService.Promocion(anio, "PRIMERO", List.of(
                new TrayectoriaService.Decision(juan.getId(), "PROMOVIDO", 2L, 20L, List.of("Matemática"), null))), "secretaria1");

        assertThat(resumen.promovidos()).isEqualTo(1);
        assertThat(resumen.areasRegistradas()).isEqualTo(1);
        Estudiante actualizado = estudiantes.findById(juan.getId()).orElseThrow();
        assertThat(actualizado.getGradoId()).isEqualTo(2L);
        assertThat(actualizado.getSeccionId()).isEqualTo(20L);

        var tray = trayectoria.trayectoria(juan.getId());
        assertThat(tray.matriculas()).extracting("anioEscolar").contains(anio, anio + 1);
        assertThat(tray.areasPendientes()).singleElement()
                .satisfies(a -> assertThat(a.estado()).isEqualTo("PENDIENTE"));

        var subsanada = trayectoria.registrarResultado(tray.areasPendientes().get(0).id(), "SUBSANADA", "A", "Carpeta de recuperación", "coord");
        assertThat(subsanada.estado()).isEqualTo("SUBSANADA");
        assertThat(trayectoria.listarAreas("PENDIENTE", null)).isEmpty();
    }

    @Test
    void recuperacionConUnAreaNoRecuperadaPromueveYDejaUnaAreaPorSubsanar() {
        int anio = TrayectoriaService.anioActual();
        trayectoria.promover(new TrayectoriaService.Promocion(anio, "PRIMERO", List.of(
                new TrayectoriaService.Decision(juan.getId(), "RECUPERACION", null, null, null,
                        List.of("Matemática", "Comunicación")))), "secretaria1");
        var enRecuperacion = trayectoria.listarAreas("PENDIENTE", "RECUPERACION");
        assertThat(enRecuperacion).hasSize(2);
        assertThat(estudiantes.findById(juan.getId()).orElseThrow().getGradoId()).isEqualTo(1L);

        // No se puede cerrar con resultados pendientes
        assertThatThrownBy(() -> trayectoria.cerrarRecuperacion(juan.getId(),
                new TrayectoriaService.CierreRecuperacion(anio, 2L, 20L, null, false), "secretaria1"))
                .hasMessageContaining("Registra primero");

        for (var area : enRecuperacion) {
            boolean recupera = area.cursoNombre().equals("Matemática");
            trayectoria.registrarResultado(area.id(), recupera ? "SUBSANADA" : "NO_SUBSANADA", recupera ? "B" : "C", null, "secretaria1");
        }
        var cierre = trayectoria.cerrarRecuperacion(juan.getId(),
                new TrayectoriaService.CierreRecuperacion(anio, 2L, 20L, null, false), "secretaria1");

        assertThat(cierre.situacion()).isEqualTo("PROMOVIDO");
        assertThat(cierre.areaPorSubsanar()).isEqualTo("Comunicación");
        assertThat(estudiantes.findById(juan.getId()).orElseThrow().getGradoId()).isEqualTo(2L);
        assertThat(trayectoria.listarAreas("PENDIENTE", "SUBSANACION")).singleElement()
                .satisfies(a -> assertThat(a.cursoNombre()).isEqualTo("Comunicación"));
        assertThat(trayectoria.trayectoria(juan.getId()).matriculas()).extracting("anioEscolar").contains(anio + 1);
    }

    @Test
    void recuperacionConDosAreasNoRecuperadasPermaneceEnElGrado() {
        int anio = TrayectoriaService.anioActual();
        trayectoria.promover(new TrayectoriaService.Promocion(anio, "TERCERO", List.of(
                new TrayectoriaService.Decision(juan.getId(), "RECUPERACION", null, null, null,
                        List.of("Matemática", "Comunicación")))), "secretaria1");
        for (var area : trayectoria.listarAreas("PENDIENTE", "RECUPERACION")) {
            trayectoria.registrarResultado(area.id(), "NO_SUBSANADA", "C", null, "secretaria1");
        }
        var cierre = trayectoria.cerrarRecuperacion(juan.getId(),
                new TrayectoriaService.CierreRecuperacion(anio, 2L, 20L, 11L, false), "secretaria1");

        assertThat(cierre.situacion()).isEqualTo("PERMANECE");
        Estudiante repite = estudiantes.findById(juan.getId()).orElseThrow();
        assertThat(repite.getGradoId()).isEqualTo(1L);
        assertThat(repite.getSeccionId()).isEqualTo(11L);
        assertThatThrownBy(() -> trayectoria.cerrarRecuperacion(juan.getId(),
                new TrayectoriaService.CierreRecuperacion(anio, 2L, 20L, null, false), "secretaria1"))
                .hasMessageContaining("ya fue cerrada");
    }

    @Test
    void elTrasladoDeSalidaConservaElHistorialYDesactivaAlEstudiante() {
        trayectoria.trasladarSalida(juan.getId(), new TrayectoriaService.TrasladoSalida(
                "IE 38001 Ayacucho", "0123456", null, "Cambio de domicilio"), "secretaria1");
        assertThat(estudiantes.findById(juan.getId()).orElseThrow().getEstado()).isEqualTo("TRASLADADO");
        assertThat(trayectoria.trayectoria(juan.getId()).matriculas()).singleElement()
                .satisfies(m -> {
                    assertThat(m.getSituacionFinal()).isEqualTo("TRASLADADO");
                    assertThat(m.getIeDestino()).isEqualTo("IE 38001 Ayacucho");
                });
        assertThatThrownBy(() -> ingresos.registrar("73123456", "ESCANER", "auxiliar1"))
                .hasMessageContaining("no está activo");
    }
}
