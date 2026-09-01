package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NivelDTO {
    private Long id;
    @NotBlank private String nombre;
    private String descripcion;
    private Boolean activo;
}
