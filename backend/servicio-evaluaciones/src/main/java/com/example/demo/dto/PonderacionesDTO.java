package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Pesos (%) de las columnas de evaluación de una sección, área y bimestre. */
@Data
public class PonderacionesDTO {
    @NotNull private Long seccionId;
    @NotNull private Long cursoId;
    private Long docenteId;
    @NotBlank private String bimestre;
    private String periodoAnio;
    @Valid @Size(max = 30) private List<Columna> columnas = new ArrayList<>();

    @Data
    public static class Columna {
        @NotBlank @Size(max = 100) private String competencia;
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") private BigDecimal peso;
    }
}
