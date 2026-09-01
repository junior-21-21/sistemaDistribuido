package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PeriodoEscolar — Bimestre o unidad del año escolar.
 * Permite "congelar" el cronograma en bloque para festividades,
 * huelgas o emergencias climáticas (contexto Huamanga, Ayacucho).
 */
@Entity
@Table(name = "periodos_escolares", indexes = {
    @Index(name = "idx_periodo_anio", columnList = "anioEscolar"),
    @Index(name = "idx_periodo_estado", columnList = "estado")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodoEscolar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Integer anioEscolar; // 2026

    @NotBlank
    @Column(nullable = false, length = 50)
    private String nombre; // "I BIMESTRE", "II BIMESTRE", ...

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaInicio;

    @NotNull
    @Column(nullable = false)
    private LocalDate fechaFin;

    /**
     * PLANIFICADO  — Próximo a iniciar
     * ACTIVO       — En curso
     * SUSPENDIDO   — Temporalmente paralizado (huelga, emergencia)
     * CERRADO      — Finalizado
     */
    @Column(nullable = false, length = 20)
    private String estado = "PLANIFICADO";

    @Column(length = 300)
    private String motivoSuspension; // Ej: "Huelga docente nacional"

    @Column(name = "fecha_suspension")
    private LocalDate fechaSuspension;

    @Column(name = "fecha_reanudacion")
    private LocalDate fechaReanudacion;

    @Column(name = "creado_por", length = 100)
    private String creadoPor;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
