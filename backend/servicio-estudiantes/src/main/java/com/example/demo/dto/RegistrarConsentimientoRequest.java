package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarConsentimientoRequest(
        @NotNull Long estudianteId,
        @NotNull Long apoderadoId,
        @NotBlank String finalidad,
        @NotBlank String accion,
        @Size(max = 300) String evidencia) { }
