package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Carga formal de un curso, docente y sección durante un año escolar. */
@Entity
@Table(name = "asignaciones_academicas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_asignacion_curso_docente_seccion_anio",
                columnNames = {"curso_id", "docente_id", "seccion_id", "anio_escolar"}),
        indexes = {
                @Index(name = "idx_asignacion_seccion", columnList = "seccion_id,anio_escolar"),
                @Index(name = "idx_asignacion_docente", columnList = "docente_id,anio_escolar")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionAcademica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @NotNull
    @Min(1)
    @Max(40)
    @Column(name = "horas_semanales", nullable = false)
    private Integer horasSemanales;

    @NotNull
    @Column(name = "anio_escolar", nullable = false)
    private Integer anioEscolar;

    @Column(nullable = false)
    private Boolean activa = true;
}
