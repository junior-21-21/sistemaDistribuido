package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SesionAsistenciaDTO {
    private Long id;
    @NotNull private Long horarioId;
    @NotNull private Long seccionId;
    @NotNull private Long cursoId;
    @NotNull private Long docenteId;
    @NotNull private LocalDate fecha;
    private String estado;
    private String abiertaPor;
    private String cerradaPor;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
}
