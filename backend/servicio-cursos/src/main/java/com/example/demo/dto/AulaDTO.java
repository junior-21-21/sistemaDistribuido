package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AulaDTO {
    private Long id;
    @NotBlank private String nombre;
    @NotBlank private String pabellon;
    @NotNull @Min(1) private Integer capacidadMaxima;
    private String turnoDisponible;
    private Boolean activa;
    private String observaciones;
}
