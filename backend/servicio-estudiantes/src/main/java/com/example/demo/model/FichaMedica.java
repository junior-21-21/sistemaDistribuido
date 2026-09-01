package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FichaMedica — Ficha de salud del estudiante gestionada por la Enfermera.
 * Incluye alergias, condiciones crónicas, tipo de sangre y vacunas.
 * La enfermera NO puede ver notas ni comportamiento — solo este módulo.
 */
@Entity
@Table(name = "fichas_medicas", indexes = {
    @Index(name = "idx_ficha_estudiante", columnList = "estudianteId", unique = true)
})
@Data
public class FichaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long estudianteId;

    // =========== DATOS MÉDICOS ===========
    @Column(nullable = false, length = 5)
    private String tipoSangre; // O+, O-, A+, A-, B+, B-, AB+, AB-

    @Column(length = 500)
    private String alergias; // "Polen, polvo, penicilina"

    @Column(length = 500)
    private String condicionesCronicas; // "Asma leve, rinitis alérgica"

    @Column(length = 300)
    private String medicamentosHabituales;

    @Column(length = 300)
    private String discapacidades;

    @Column(length = 300)
    private String observacionesGenerales;

    // =========== SEGURO / COBERTURA ===========
    @Column(length = 20)
    private String tipoSeguro; // SIS, ESSALUD, PRIVADO, NINGUNO

    @Column(length = 50)
    private String nroSeguro;

    // =========== DATOS DE CONTACTO EN EMERGENCIA ===========
    @Column(length = 100)
    private String contactoEmergenciaNombre;

    @Column(length = 15)
    private String contactoEmergenciaTelefono;

    @Column(length = 50)
    private String contactoEmergenciaParentesco;

    // =========== AUDITORÍA ===========
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "ultima_actualizacion_por", length = 100)
    private String ultimaActualizacionPor; // username de la enfermera

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
