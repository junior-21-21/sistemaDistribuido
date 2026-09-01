package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PeriodoEscolarDTO {
    private Long id;
    @NotNull private Integer anioEscolar;
    @NotBlank private String nombre;
    @NotNull private LocalDate fechaInicio;
    @NotNull private LocalDate fechaFin;
    private String estado;
    private String motivoSuspension;
    private LocalDate fechaSuspension;
    private LocalDate fechaReanudacion;
    private String creadoPor;
}
