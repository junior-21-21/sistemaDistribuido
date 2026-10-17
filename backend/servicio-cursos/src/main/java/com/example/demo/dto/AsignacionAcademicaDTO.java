package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AsignacionAcademicaDTO {
    private Long id;
    @NotNull private Long cursoId;
    private String cursoNombre;
    @NotNull private Long docenteId;
    private String docenteNombre;
    @NotNull private Long seccionId;
    private String seccionNombre;
    @NotNull @Min(1) @Max(40) private Integer horasSemanales;
    @NotNull private Integer anioEscolar;
    private Boolean activa = true;
    private Integer horasContratadasDocente;
    private Integer horasAsignadasDocente;
    private Integer horasDisponiblesDocente;
}
