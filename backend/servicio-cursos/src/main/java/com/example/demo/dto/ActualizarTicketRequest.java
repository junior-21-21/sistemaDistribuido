package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarTicketRequest(
        @NotBlank @Pattern(regexp = "PENDIENTE|EN_PROCESO|RESUELTO") String estado,
        @Size(max = 300) String notasResolucion) {
}
