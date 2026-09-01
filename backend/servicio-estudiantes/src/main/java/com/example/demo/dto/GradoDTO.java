package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GradoDTO {
    private Long id;

    @NotBlank(message = "El nombre del grado es obligatorio")
    private String nombre;

    @NotNull(message = "El ID del nivel es obligatorio")
    private Long nivelId;
    
    private String nivelNombre;

    public GradoDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getNivelId() { return nivelId; }
    public void setNivelId(Long nivelId) { this.nivelId = nivelId; }
    public String getNivelNombre() { return nivelNombre; }
    public void setNivelNombre(String nivelNombre) { this.nivelNombre = nivelNombre; }
}
