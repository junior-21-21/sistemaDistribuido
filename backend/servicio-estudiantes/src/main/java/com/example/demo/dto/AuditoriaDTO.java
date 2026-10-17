package com.example.demo.dto;

import java.time.Instant;
import java.util.Map;

public record AuditoriaDTO(
        int revision,
        Instant fecha,
        String accion,
        String entidad,
        Long entidadId,
        String usuario,
        String correlationId,
        Map<String, Object> valores) {
}
