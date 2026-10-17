package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReporteDisciplinarioDTO {

    private Long id;

    @NotNull
    private Long estudianteId;

    private Long docenteId;

    @NotNull
    private Long seccionId;

    @NotBlank
    @Pattern(regexp = "TARDANZA|FALTA_LEVE|FALTA_GRAVE|MERITO|LLAMADA_ATENCION|VIOLENCIA_ESCOLAR", message = "Tipo de reporte no válido")
    private String tipo;

    @NotBlank
    @Size(max = 600)
    private String descripcion;

    private String estado;
    private Boolean apoderadoNotificado;
    private LocalDateTime fechaReporte;
    private LocalDateTime fechaNotificacion;
    private String registradoPor;

    @Size(max = 300)
    private String observaciones;

    // Protocolo de violencia escolar (solo lectura al crear; se completa con PATCH /protocolo)
    private String codigoSiseve;
    private java.time.LocalDate fechaSiseve;
    private LocalDateTime citacionApoderado;
    private String medidasProteccion;
    private String derivacionExterna;
    private java.time.LocalDate fechaDerivacion;
    private String protocoloActualizadoPor;
}
