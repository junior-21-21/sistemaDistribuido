package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Pasos del protocolo de atención de violencia escolar; los campos vacíos no se modifican. */
public record ProtocoloViolenciaRequest(
        @Size(max = 30) String codigoSiseve,
        LocalDate fechaSiseve,
        LocalDateTime citacionApoderado,
        @Size(max = 500) String medidasProteccion,
        @Pattern(regexp = "^$|PSICOLOGIA|DEMUNA|CEM|POLICIA|FISCALIA|CENTRO_SALUD", message = "Derivación no válida")
        String derivacionExterna,
        LocalDate fechaDerivacion) {
}
