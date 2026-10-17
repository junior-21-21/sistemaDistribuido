package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeccionDTO {
    private Long id;
    @NotBlank private String nombre;
    @NotNull private Long gradoId;
    private String gradoNombre;
    private String nivelNombre;
    @NotNull private String turno;
    @Min(1) private Integer capacidadMaxima;
    private Boolean activa;
    private Integer anioEscolar;
    private Long docenteTutorId;
    private String docenteTutorNombre;
}
