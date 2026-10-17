package com.example.demo.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.repository.AsistenciaRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Lo llama servicio-estudiantes cuando se aprueba una justificación o un permiso. */
@RestController
@RequestMapping("/api/internal/asistencias")
public class InternalJustificacionController {

    public record JustificarRequest(@NotNull Long estudianteId, @NotNull LocalDate desde, @NotNull LocalDate hasta,
                                    @Size(max = 400) String motivo) {
    }

    private final AsistenciaRepository repository;
    private final String expectedKey;

    public InternalJustificacionController(AsistenciaRepository repository,
                                           @Value("${app.internal-api-key}") String expectedKey) {
        this.repository = repository;
        this.expectedKey = expectedKey;
    }

    @PostMapping("/justificar")
    @Transactional
    public Map<String, Integer> justificar(@Valid @RequestBody JustificarRequest request,
                                           @RequestHeader("X-Internal-Api-Key") String providedKey) {
        if (!MessageDigest.isEqual(expectedKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Clave interna inválida");
        }
        if (request.hasta().isBefore(request.desde())) {
            throw new IllegalArgumentException("Rango de fechas inválido");
        }
        String motivo = request.motivo() == null ? "Justificación aprobada" : request.motivo();
        if (motivo.length() > 255) motivo = motivo.substring(0, 255);
        int total = repository.justificar(request.estudianteId(), request.desde(), request.hasta(), motivo);
        return Map.of("justificadas", total);
    }
}
