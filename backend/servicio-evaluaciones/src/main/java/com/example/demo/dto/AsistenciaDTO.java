package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AsistenciaDTO {
    private Long id;

    @NotNull
    private LocalDate fecha;

    @NotBlank
    @Pattern(regexp = "PRESENTE|FALTA|TARDE|JUSTIFICADO", message = "Estado de asistencia no válido")
    private String estado;

    @NotNull
    private Long estudianteId;

    @NotNull
    private Long horarioId;

    @NotNull
    private Long docenteId;

    @NotNull
    private Long seccionId;

    @NotNull
    private Long cursoId;

    private String turno;

    @Size(max = 300)
    private String justificacion;

    private String registradoPor;

    private LocalDateTime fechaRegistro;

    private LocalDateTime fechaModificacion;
}
