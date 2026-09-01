package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReporteDisciplinarioDTO {

    private Long id;

    @NotNull
    private Long estudianteId;

    @NotNull
    private Long docenteId;

    @NotBlank
    private String tipo;

    @NotBlank
    private String descripcion;

    private LocalDateTime fechaReporte;
}
