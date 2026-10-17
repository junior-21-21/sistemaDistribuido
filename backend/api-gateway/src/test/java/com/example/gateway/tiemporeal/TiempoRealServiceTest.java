package com.example.gateway.tiemporeal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

class TiempoRealServiceTest {

    private final TiempoRealService service = new TiempoRealService();

    @Test
    void extraeElModuloDeLaRuta() {
        assertEquals("notas", CambiosGlobalFilter.modulo("/api/notas/15"));
        assertEquals("documentos", CambiosGlobalFilter.modulo("/api/estudiantes/3/documentos"));
        assertNull(CambiosGlobalFilter.modulo("/api/auth/login"));
        assertNull(CambiosGlobalFilter.modulo("/actuator/health"));
    }

    @Test
    void soloLosRolesAutorizadosRecibenElCambio() {
        assertTrue(service.puedeVer("fichas-medicas", List.of("ENFERMERA", "PRIMEROS_AUXILIOS")));
        assertFalse(service.puedeVer("fichas-medicas", List.of("DOCENTE", "DIRECTOR")));
        assertTrue(service.puedeVer("tickets", List.of("SECRETARIA", "MANTENIMIENTO")));
        assertFalse(service.puedeVer("modulo-desconocido", List.of("DOCENTE")));
    }

    @Test
    void entregaEventosEnVivoConDescripcionLegible() {
        StepVerifier.create(service.eventosPara(List.of("DOCENTE")).take(1))
                .then(() -> {
                    service.publicar("fichas-medicas", "POST", "Enfermera", "enfermera1");
                    service.publicar("notas", "POST", "Ana Flores", "docente1");
                })
                .assertNext(evento -> {
                    assertEquals("notas", evento.modulo());
                    assertEquals("CREAR", evento.accion());
                    assertEquals("registró notas", evento.descripcion());
                    assertEquals("Ana Flores", evento.actor());
                })
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }
}
