package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearPermisoRequest(
        @NotNull Long estudianteId,
        @NotBlank @Pattern(regexp = "JUSTIFICACION|PAPELETA_SALIDA|PERMISO", message = "Tipo de solicitud no válido") String tipo,
        @NotNull LocalDate fechaDesde,
        LocalDate fechaHasta,
        LocalTime horaSalida,
        @NotBlank @Size(min = 5, max = 400) String motivo,
        @Size(max = 150) String personaRecoge,
        @Pattern(regexp = "^$|\\d{8}", message = "El DNI de quien recoge debe tener 8 dígitos") String dniPersonaRecoge,
        /** Quien puede aprobar lo registra ya aprobado (por ejemplo, el auxiliar con el apoderado delante). */
        Boolean aprobarAhora) {
}
