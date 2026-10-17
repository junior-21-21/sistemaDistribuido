package com.example.reportes.controller;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.reportes.service.AlertaDesercionService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private static final Duration TIMEOUT_SERVICIO = Duration.ofSeconds(3);

    private final WebClient estudiantesClient;
    private final WebClient cursosClient;
    private final AlertaDesercionService alertaDesercionService;

    public EstadisticasController(
            @Qualifier("estudiantesClient") WebClient estudiantesClient,
            @Qualifier("cursosClient") WebClient cursosClient,
            AlertaDesercionService alertaDesercionService) {
        this.estudiantesClient = estudiantesClient;
        this.cursosClient = cursosClient;
        this.alertaDesercionService = alertaDesercionService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getDashboardStats() {
        Mono<Long> totalEstudiantes = consultarConteo(
                estudiantesClient, "/api/estudiantes/count/total");
        Mono<Long> matriculasPendientes = consultarConteo(
                estudiantesClient, "/api/estudiantes/count/pendientes");
        Mono<Long> docentesActivos = consultarConteo(
                cursosClient, "/api/docentes/count");
        Mono<Long> alertasActivas = alertaDesercionService.contarAlertasActivas();

        return Mono.zip(totalEstudiantes, matriculasPendientes, docentesActivos, alertasActivas)
                .map(conteos -> {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("totalEstudiantes", conteos.getT1());
                    stats.put("matriculasPendientes", conteos.getT2());
                    stats.put("docentesActivos", conteos.getT3());
                    stats.put("alertasDesercion", conteos.getT4());
                    stats.put("mensaje", "Datos sincronizados con los microservicios");
                    return ResponseEntity.ok(stats);
                });
    }

    private Mono<Long> consultarConteo(WebClient client, String uri) {
        return client.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Long.class)
                .timeout(TIMEOUT_SERVICIO)
                .onErrorReturn(0L);
    }
}

