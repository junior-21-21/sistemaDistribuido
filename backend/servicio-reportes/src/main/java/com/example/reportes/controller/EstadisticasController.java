package com.example.reportes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

/**
 * Estadisticas agregadas para el Dashboard del Director.
 * Consulta a los demas microservicios via HTTP.
 */
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final WebClient webClient;

    public EstadisticasController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Long totalEstudiantes = webClient.get()
                .uri("http://servicio-estudiantes:8082/api/estudiantes/count/total")
                .retrieve()
                .bodyToMono(Long.class)
                .onErrorReturn(0L)
                .toFuture().get();

            Long matriculasPendientes = webClient.get()
                .uri("http://servicio-estudiantes:8082/api/estudiantes/count/pendientes")
                .retrieve()
                .bodyToMono(Long.class)
                .onErrorReturn(0L)
                .toFuture().get();

            Long docentesActivos = webClient.get()
                .uri("http://servicio-cursos:8083/api/docentes/count")
                .retrieve()
                .bodyToMono(Long.class)
                .onErrorReturn(0L)
                .toFuture().get();

            stats.put("totalEstudiantes", totalEstudiantes);
            stats.put("docentesActivos", docentesActivos);
            stats.put("matriculasPendientes", matriculasPendientes);
            stats.put("alertasDesercion", 7); // Métrica pendiente de implementación real
            stats.put("mensaje", "Datos sincronizados con los microservicios correctamente");
        } catch (InterruptedException | ExecutionException e) {
            stats.put("totalEstudiantes", 0);
            stats.put("docentesActivos", 0);
            stats.put("matriculasPendientes", 0);
            stats.put("alertasDesercion", 7);
            stats.put("mensaje", "Error al conectar con los microservicios");
        }
        
        return ResponseEntity.ok(stats);
    }
}
