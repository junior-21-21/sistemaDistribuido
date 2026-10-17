package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FichaMedicaRequest(
        @NotNull Long estudianteId,
        @NotBlank
        @Pattern(regexp = "O[+-]|A[+-]|B[+-]|AB[+-]", message = "Tipo de sangre inválido")
        String tipoSangre,
        @Size(max = 500) String alergias,
        @Size(max = 500) String condicionesCronicas,
        @Size(max = 300) String medicamentosHabituales,
        @Size(max = 300) String discapacidades,
        @Size(max = 300) String observacionesGenerales,
        @Pattern(regexp = "SIS|ESSALUD|PRIVADO|NINGUNO", message = "Tipo de seguro inválido")
        String tipoSeguro,
        @Size(max = 50) String nroSeguro,
        @Size(max = 100) String contactoEmergenciaNombre,
        @Pattern(regexp = "[0-9+() -]{7,15}", message = "Teléfono de emergencia inválido")
        String contactoEmergenciaTelefono,
        @Size(max = 50) String contactoEmergenciaParentesco) {
}
