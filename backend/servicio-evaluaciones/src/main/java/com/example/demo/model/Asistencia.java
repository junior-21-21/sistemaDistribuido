package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Asistencia — Registro de asistencia diaria por sesión de clase.
 * Estados: PRESENTE, FALTA, TARDE, JUSTIFICADO
 * Una falta JUSTIFICADA no cuenta para alertas de deserción.
 */
@Entity
@Table(name = "asistencias", uniqueConstraints = @UniqueConstraint(
    name = "uk_asistencia_estudiante_sesion", columnNames = {"estudianteId", "horarioId", "fecha"}), indexes = {
    @Index(name = "idx_asistencia_estudiante", columnList = "estudianteId"),
    @Index(name = "idx_asistencia_fecha", columnList = "fecha"),
    @Index(name = "idx_asistencia_horario", columnList = "horarioId"),
    @Index(name = "idx_asistencia_seccion", columnList = "seccionId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La fecha es obligatoria")
    @Column(nullable = false)
    private LocalDate fecha;

    /**
     * PRESENTE     — Alumno asistió puntualmente
     * TARDE        — Llegó tarde (genera registro de tardanza en disciplina)
     * FALTA        — Inasistencia no justificada
     * JUSTIFICADO  — Falta con justificación aprobada (no penaliza)
     */
    @NotNull(message = "El estado es obligatorio")
    @Column(nullable = false, length = 20)
    private String estado;

    @NotNull
    @Column(nullable = false)
    private Long estudianteId;

    @NotNull
    @Column(nullable = false)
    private Long horarioId;

    @Column(nullable = false)
    private Long docenteId;   // Docente que tomó asistencia

    @Column(nullable = false)
    private Long seccionId;   // Sección a la que pertenece el estudiante

    @Column(nullable = false)
    private Long cursoId;     // Curso al que corresponde la sesión

    @Column(length = 20)
    private String turno;     // MANANA, TARDE

    @Column(length = 300)
    private String justificacion; // Motivo de falta justificada

    @Column(name = "registrado_por", length = 100)
    private String registradoPor; // Username del docente

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
