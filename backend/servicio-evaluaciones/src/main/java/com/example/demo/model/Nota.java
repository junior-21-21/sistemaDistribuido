package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Nota — Registro de evaluación por competencias MINEDU.
 * Escala literal: AD (Logro Destacado), A (Logro Esperado), B (En Proceso), C (En Inicio)
 * También soporta escala vigesimal (0-20) para niveles que lo requieren.
 */
@Entity
@Table(name = "notas", indexes = {
    @Index(name = "idx_nota_estudiante", columnList = "estudianteId"),
    @Index(name = "idx_nota_curso", columnList = "cursoId"),
    @Index(name = "idx_nota_docente", columnList = "docenteId")
})
@Data
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========== ESCALA LITERAL (MINEDU - Primaria y Secundaria) ===========
    /**
     * AD = Logro Destacado (supera lo esperado)
     * A  = Logro Esperado (dentro de lo esperado)
     * B  = En Proceso (cerca de lograr)
     * C  = En Inicio (dificultad significativa)
     */
    @Column(length = 2)
    private String escalaLiteral; // AD, A, B, C

    // =========== ESCALA VIGESIMAL (Para niveles que aún la usan) ===========
    @Column(precision = 4)
    private Double valorNumerico; // 0.0 a 20.0

    // =========== METADATOS DE LA EVALUACIÓN ===========
    @Column(nullable = false, length = 100)
    private String competencia; // "Comunicación", "Matemática", "Ciencia y Tecnología"

    @Column(nullable = false, length = 50)
    private String bimestre; // "I BIMESTRE", "II BIMESTRE", "III BIMESTRE", "IV BIMESTRE"

    @Column(nullable = false, length = 20)
    private String periodoAnio; // "2026"

    // =========== REFERENCIAS ===========
    @Column(nullable = false)
    private Long estudianteId;

    @Column(nullable = false)
    private Long cursoId;

    @Column(nullable = false)
    private Long docenteId;

    @Column(nullable = false)
    private Long seccionId;

    // =========== AUDITORÍA ===========
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
        if (this.periodoAnio == null || this.periodoAnio.isBlank()) {
            this.periodoAnio = String.valueOf(java.time.LocalDate.now().getYear());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }

    /**
     * Convierte escala literal a valor numérico referencial (para promedios)
     */
    public Double getValorCalculado() {
        if (escalaLiteral != null) {
            return switch (escalaLiteral) {
                case "AD" -> 18.0;
                case "A"  -> 15.0;
                case "B"  -> 12.0;
                case "C"  -> 9.0;
                default   -> 0.0;
            };
        }
        return valorNumerico != null ? valorNumerico : 0.0;
    }
}
