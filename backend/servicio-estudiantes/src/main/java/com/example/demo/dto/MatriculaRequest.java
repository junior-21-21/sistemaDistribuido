package com.example.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MatriculaRequest(
        @Valid @NotNull EstudianteDTO estudiante,
        @NotNull Long apoderadoId,
        @NotBlank String parentesco,
        Boolean permisoRetiro,
        @Valid List<@Valid ConsentimientoInicialRequest> consentimientos,
        /** NUEVO_INGRESO, CONTINUIDAD o TRASLADO_ENTRANTE (por defecto NUEVO_INGRESO). */
        String tipoMatricula,
        String ieProcedencia,
        String codigoModularProcedencia) {
}
