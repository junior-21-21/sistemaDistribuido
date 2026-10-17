package com.example.reportes.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.reportes.dto.AlertaDesercionDTO;

import reactor.core.publisher.Mono;

@Service
public class AlertaDesercionService {

    private final WebClient evaluacionesClient;
    private final WebClient estudiantesClient;

    public AlertaDesercionService(
            @Qualifier("evaluacionesClient") WebClient evaluacionesClient,
            @Qualifier("estudiantesClient") WebClient estudiantesClient) {
        this.evaluacionesClient = evaluacionesClient;
        this.estudiantesClient = estudiantesClient;
    }

    public Mono<List<AlertaDesercionDTO>> generarAlertas(Long seccionId) {
        LocalDate desde = LocalDate.now().minusDays(14);
        Mono<List<RiesgoAsistencia>> riesgos = evaluacionesClient.get()
                .uri(builder -> {
                    builder.path("/api/asistencias/riesgo").queryParam("desde", desde);
                    if (seccionId != null) builder.queryParam("seccionId", seccionId);
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<RiesgoAsistencia>>() {})
                .onErrorReturn(List.of());

        Mono<List<EstudianteResumen>> estudiantes = estudiantesClient.get()
                .uri("/api/estudiantes")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<EstudianteResumen>>() {})
                .onErrorReturn(List.of());

        return Mono.zip(riesgos, estudiantes)
                .map(datos -> datos.getT1().stream()
                        .map(riesgo -> crearAlerta(riesgo, datos.getT2()))
                        .toList());
    }

    public Mono<Long> contarAlertasActivas() {
        // Una sola falta en dos semanas (BAJO) no es riesgo de deserción para el indicador
        return generarAlertas(null).map(alertas -> alertas.stream()
                .filter(alerta -> !"BAJO".equals(alerta.getNivel()))
                .count());
    }

    private AlertaDesercionDTO crearAlerta(
            RiesgoAsistencia riesgo,
            List<EstudianteResumen> estudiantes) {
        EstudianteResumen estudiante = estudiantes.stream()
                .filter(item -> item.id().equals(riesgo.estudianteId()))
                .findFirst()
                .orElse(null);
        int faltas = Math.toIntExact(riesgo.faltasRecientes());
        String nivel = faltas >= 4 ? "ALTO" : faltas >= 2 ? "MEDIO" : "BAJO";
        return AlertaDesercionDTO.builder()
                .estudianteId(riesgo.estudianteId())
                .nombreEstudiante(estudiante == null
                        ? "Estudiante " + riesgo.estudianteId()
                        : estudiante.nombre() + " " + estudiante.apellido())
                .nivel(nivel)
                .motivo(faltas + (faltas == 1 ? " día con falta no justificada" : " días con faltas no justificadas")
                        + " en los últimos 14 días")
                .faltasRecientes(faltas)
                .ultimaAsistencia(riesgo.ultimaFalta().toString())
                .seccionId(riesgo.seccionId())
                .seccionNombre(estudiante == null || estudiante.seccionNombre() == null
                        ? "Sección " + riesgo.seccionId()
                        : estudiante.seccionNombre())
                .build();
    }

    private record RiesgoAsistencia(
            Long estudianteId,
            Long seccionId,
            long faltasRecientes,
            LocalDate ultimaFalta) {
    }

    private record EstudianteResumen(
            Long id,
            String nombre,
            String apellido,
            String estado,
            Long seccionId,
            String seccionNombre) {
    }
}
