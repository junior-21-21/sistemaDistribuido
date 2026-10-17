package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearDerivacionRequest(
        @NotNull @Positive Long estudianteId,
        @NotBlank @Pattern(regexp = "COMPORTAMIENTO|RENDIMIENTO|BULLYING|FAMILIAR|OTRO") String motivo,
        @NotBlank @Size(max = 600) String descripcion,
        @NotBlank @Pattern(regexp = "NORMAL|URGENTE") String prioridad) {
}
