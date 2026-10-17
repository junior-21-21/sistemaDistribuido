package com.example.reportes.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import com.example.reportes.model.Actividad;
import com.example.reportes.repository.ActividadRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/actividades")
public class InternalActividadController {
    private final ActividadRepository repository;
    private final byte[] internalKey;

    public InternalActividadController(
            ActividadRepository repository,
            @Value("${app.internal-api-key}") String internalKey) {
        this.repository = repository;
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping
    public ResponseEntity<Void> registrar(
            @RequestHeader("X-Internal-Key") String suppliedKey,
            @RequestBody Actividad actividad) {
        if (!MessageDigest.isEqual(
                internalKey,
                suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        actividad.setId(null);
        if (actividad.getFecha() == null) {
            actividad.setFecha(LocalDateTime.now());
        }
        repository.save(actividad);
        return ResponseEntity.noContent().build();
    }
}
