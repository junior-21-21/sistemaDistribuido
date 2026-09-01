package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class NotaDTO {
    private Long id;

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "20.0")
    private BigDecimal valor;

    @NotBlank
    private String bimestre;

    @NotNull
    private Long estudianteId;

    @NotNull
    private Long cursoId;

    @NotNull
    private Long docenteId;

    private LocalDateTime fechaRegistro;
}
