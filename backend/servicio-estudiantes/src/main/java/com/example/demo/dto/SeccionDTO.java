package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SeccionDTO {
    private Long id;

    @NotBlank(message = "El nombre de la sección es obligatorio")
    @Size(max = 5, message = "El nombre no puede exceder 5 caracteres")
    private String nombre;

    @NotNull(message = "El ID del grado es obligatorio")
    private Long gradoId;
    
    private String gradoNombre;

    @NotNull(message = "El ID del periodo es obligatorio")
    private Long periodoId;

    private Long aulaId;
    
    private Integer cupoMaximo;
    
    private Integer cupoOcupado;
    
    public SeccionDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getGradoId() { return gradoId; }
    public void setGradoId(Long gradoId) { this.gradoId = gradoId; }
    public String getGradoNombre() { return gradoNombre; }
    public void setGradoNombre(String gradoNombre) { this.gradoNombre = gradoNombre; }
    public Long getPeriodoId() { return periodoId; }
    public void setPeriodoId(Long periodoId) { this.periodoId = periodoId; }
    public Long getAulaId() { return aulaId; }
    public void setAulaId(Long aulaId) { this.aulaId = aulaId; }
    public Integer getCupoMaximo() { return cupoMaximo; }
    public void setCupoMaximo(Integer cupoMaximo) { this.cupoMaximo = cupoMaximo; }
    public Integer getCupoOcupado() { return cupoOcupado; }
    public void setCupoOcupado(Integer cupoOcupado) { this.cupoOcupado = cupoOcupado; }
}
