package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** Nómina de matrícula de una sección aprobada por Dirección (RM 432-2020-MINEDU). */
@Entity
@Table(name = "nominas_matricula")
@Data
public class NominaMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seccion_id", nullable = false)
    private Long seccionId;

    @Column(name = "anio_escolar", nullable = false)
    private Integer anioEscolar;

    /** Estudiantes en la sección al momento de aprobar; si cambia, la nómina queda desactualizada. */
    @Column(name = "total_estudiantes", nullable = false)
    private Integer totalEstudiantes;

    @Column(name = "aprobada_por", nullable = false, length = 100)
    private String aprobadaPor;

    @Column(name = "fecha_aprobacion", nullable = false)
    private LocalDateTime fechaAprobacion = LocalDateTime.now();

    @Column(length = 300)
    private String observacion;
}
