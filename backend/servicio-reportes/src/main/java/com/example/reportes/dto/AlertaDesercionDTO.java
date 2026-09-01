package com.example.reportes.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * DTO de alerta de riesgo de desercion.
 * nivel: ALTO, MEDIO, BAJO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaDesercionDTO {
    private Long estudianteId;
    private String nombreEstudiante;
    private String nivel; // ALTO, MEDIO, BAJO
    private String motivo; // "3 faltas en 2 semanas", "Promedio inferior a B"
    private Integer faltasRecientes;
    private String ultimaAsistencia;
    private Long seccionId;
    private String seccionNombre;
}
