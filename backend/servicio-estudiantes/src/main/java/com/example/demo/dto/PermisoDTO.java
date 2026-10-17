package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Solicitud de justificación, papeleta de salida o permiso con los datos del estudiante para mostrarla. */
public record PermisoDTO(
        Long id,
        Long estudianteId,
        String estudianteNombre,
        String estudianteDni,
        Long seccionId,
        String tipo,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        LocalTime horaSalida,
        String motivo,
        String personaRecoge,
        String dniPersonaRecoge,
        String origen,
        String solicitadoPor,
        String estado,
        String revisadoPor,
        LocalDateTime fechaRevision,
        String observacionRevision,
        int asistenciasJustificadas,
        String salidaRegistradaPor,
        LocalDateTime fechaSalida,
        LocalDateTime creadoEn) {
}
