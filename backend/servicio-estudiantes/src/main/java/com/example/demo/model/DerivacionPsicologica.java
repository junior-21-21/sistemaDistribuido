package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DerivacionPsicologica — Módulo de Tutoría y Psicología.
 * Registro confidencial de derivaciones al psicólogo del colegio.
 */
@Entity
@Table(name = "derivaciones_psicologicas", indexes = {
    @Index(name = "idx_deriv_estudiante", columnList = "estudianteId"),
    @Index(name = "idx_deriv_docente", columnList = "docenteDerivadorId")
})
@Data
public class DerivacionPsicologica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long estudianteId;

    @Column(nullable = false)
    private Long docenteDerivadorId;

    @Column(nullable = false, length = 50)
    private String motivo; // COMPORTAMIENTO, RENDIMIENTO, BULLYING, FAMILIAR, OTRO

    @Column(nullable = false, length = 600)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String prioridad; // NORMAL, URGENTE

    @Column(nullable = false, length = 20)
    private String estado = "RECIBIDO"; // RECIBIDO, EN_EVALUACION, CONCLUIDO

    @Column(name = "fecha_derivacion", nullable = false, updatable = false)
    private LocalDateTime fechaDerivacion;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    // Solo visible para el psicólogo/director
    @Column(length = 600)
    private String notasPsicologo; 

    @PrePersist
    protected void onCreate() {
        this.fechaDerivacion = LocalDateTime.now();
        if (this.estado == null) this.estado = "RECIBIDO";
    }
}
