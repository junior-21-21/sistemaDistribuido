package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtenderDerivacionRequest(
        @NotBlank @Pattern(regexp = "RECIBIDO|EN_EVALUACION|CONCLUIDO") String estado,
        @Size(max = 600) String notasPsicologo) {
}
