package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotaDTO {
    private Long id;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "20.0")
    private Double valorNumerico;

    @Pattern(regexp = "AD|A|B|C", message = "La escala literal debe ser AD, A, B o C")
    private String escalaLiteral;

    @NotBlank
    @Size(max = 100)
    private String competencia;

    @NotBlank
    private String bimestre;

    @NotNull
    private Long estudianteId;

    @NotNull
    private Long cursoId;

    @NotNull
    private Long docenteId;

    @NotNull
    private Long seccionId;

    @Size(max = 20)
    private String periodoAnio;

    @Size(max = 500)
    private String observaciones;

    private LocalDateTime fechaRegistro;

    private LocalDateTime fechaModificacion;

    @JsonIgnore
    @AssertTrue(message = "Debe registrar una escala literal o un valor numérico")
    public boolean isCalificacionPresente() {
        return escalaLiteral != null || valorNumerico != null;
    }
}
