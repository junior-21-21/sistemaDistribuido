package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearAtencionMedicaRequest(
        @NotNull @Positive Long fichaMedicaId,
        @NotBlank @Size(max = 200) String motivo,
        @NotBlank @Size(max = 800) String diagnostico,
        @NotBlank @Size(max = 800) String tratamiento,
        Boolean apoderadoAvisado,
        @Pattern(regexp = "RETORNA_AULA|RETIRADO_APODERADO|TRASLADO_SALUD", message = "Destino no válido") String destino,
        @Size(max = 150) String establecimientoSalud,
        Boolean cubiertoSis) {
}
