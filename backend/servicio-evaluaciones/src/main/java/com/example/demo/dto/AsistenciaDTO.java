package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AsistenciaDTO {
    private Long id;

    @NotNull
    private LocalDate fecha;

    @NotBlank
    private String estado;

    @NotNull
    private Long estudianteId;

    @NotNull
    private Long horarioId;

    private Long docenteId;

    private Long seccionId;

    private Long cursoId;

    private String turno;

    private String justificacion;

    private String registradoPor;

    private LocalDateTime fechaRegistro;
}
