package com.example.demo.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AsistenciaDTO;
import com.example.demo.dto.NotaDTO;
import com.example.demo.service.AsistenciaService;
import com.example.demo.service.NotaService;

@RestController
@RequestMapping("/api/internal/portal/estudiantes")
public class InternalPortalController {

    private final NotaService notaService;
    private final AsistenciaService asistenciaService;
    private final String expectedKey;

    public InternalPortalController(
            NotaService notaService,
            AsistenciaService asistenciaService,
            @Value("${app.internal-api-key}") String expectedKey) {
        this.notaService = notaService;
        this.asistenciaService = asistenciaService;
        this.expectedKey = expectedKey;
    }

    @GetMapping("/{estudianteId}/notas")
    public List<NotaDTO> notas(
            @PathVariable Long estudianteId,
            @RequestHeader("X-Internal-Api-Key") String providedKey) {
        validarClave(providedKey);
        return notaService.listarPorEstudiante(estudianteId);
    }

    @GetMapping("/{estudianteId}/asistencias")
    public List<AsistenciaDTO> asistencias(
            @PathVariable Long estudianteId,
            @RequestHeader("X-Internal-Api-Key") String providedKey) {
        validarClave(providedKey);
        return asistenciaService.listarPorEstudiante(estudianteId);
    }

    private void validarClave(String providedKey) {
        boolean valida = MessageDigest.isEqual(expectedKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8));
        if (!valida) throw new org.springframework.security.access.AccessDeniedException("Clave interna inválida");
    }
}
