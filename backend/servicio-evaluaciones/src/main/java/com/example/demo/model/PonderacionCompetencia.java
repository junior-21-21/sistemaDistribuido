package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

/** Peso (%) de una columna de evaluación dentro del promedio del bimestre. */
@Entity
@Table(name = "ponderaciones_competencia", uniqueConstraints =
        @UniqueConstraint(name = "uk_ponderacion_columna",
                columnNames = {"seccion_id", "curso_id", "bimestre", "periodo_anio", "competencia"}))
@Data
public class PonderacionCompetencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "seccion_id", nullable = false) private Long seccionId;
    @Column(name = "curso_id", nullable = false) private Long cursoId;
    @Column(nullable = false, length = 20) private String bimestre;
    @Column(name = "periodo_anio", nullable = false, length = 4) private String periodoAnio;
    @Column(nullable = false, length = 100) private String competencia;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal peso;
    @Column(nullable = false) private Integer orden = 0;
    @Column(name = "actualizado_por", nullable = false, length = 100) private String actualizadoPor;
    @Column(name = "actualizado_en", nullable = false) private LocalDateTime actualizadoEn;
}
