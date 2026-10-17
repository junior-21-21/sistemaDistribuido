package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearTicketRequest(
        @NotNull @Positive Long aulaId,
        @NotBlank @Pattern(regexp = "MOBILIARIO|ELECTRICO|PLOMERIA|TECNOLOGIA|OTROS") String categoria,
        @NotBlank @Size(max = 500) String descripcion,
        @NotBlank @Pattern(regexp = "BAJA|MEDIA|ALTA|EMERGENCIA") String urgencia) {
}
