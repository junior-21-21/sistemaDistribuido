package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GradoDTO {
    private Long id;
    @NotBlank private String nombre;
    @NotNull private Long nivelId;
    private String nivelNombre;
    private Boolean activo;
}
