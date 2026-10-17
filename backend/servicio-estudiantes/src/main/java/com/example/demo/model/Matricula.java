package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** Matrícula de un estudiante en un año escolar (equivale al registro anual del SIAGIE). */
@Entity
@Table(name = "matriculas")
@Data
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(name = "anio_escolar", nullable = false)
    private Integer anioEscolar;

    /** NUEVO_INGRESO, CONTINUIDAD o TRASLADO_ENTRANTE. */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "grado_id")
    private Long gradoId;

    @Column(name = "seccion_id")
    private Long seccionId;

    @Column(name = "ie_procedencia", length = 150)
    private String ieProcedencia;

    @Column(name = "codigo_modular_procedencia", length = 12)
    private String codigoModularProcedencia;

    /** PROMOVIDO, PERMANECE, RECUPERACION, EGRESADO, TRASLADADO o RETIRADO. */
    @Column(name = "situacion_final", length = 20)
    private String situacionFinal;

    @Column(name = "ie_destino", length = 150)
    private String ieDestino;

    @Column(name = "codigo_modular_destino", length = 12)
    private String codigoModularDestino;

    @Column(name = "fecha_traslado")
    private LocalDate fechaTraslado;

    @Column(name = "motivo_traslado", length = 300)
    private String motivoTraslado;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "registrado_por", nullable = false, length = 100)
    private String registradoPor;
}
