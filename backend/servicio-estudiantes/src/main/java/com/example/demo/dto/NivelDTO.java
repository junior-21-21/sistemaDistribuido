package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class NivelDTO {
    private Long id;

    @NotBlank(message = "El nombre del nivel es obligatorio")
    private String nombre;

    public NivelDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
