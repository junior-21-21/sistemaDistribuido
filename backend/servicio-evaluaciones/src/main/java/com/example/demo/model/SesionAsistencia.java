package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_asistencia", uniqueConstraints = @UniqueConstraint(
        name = "uk_sesion_asistencia_horario_fecha", columnNames = {"horario_id", "fecha"}),
        indexes = @Index(name = "idx_sesion_seccion_fecha", columnList = "seccion_id,fecha"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SesionAsistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "horario_id", nullable = false) private Long horarioId;
    @Column(name = "seccion_id", nullable = false) private Long seccionId;
    @Column(name = "curso_id", nullable = false) private Long cursoId;
    @Column(name = "docente_id", nullable = false) private Long docenteId;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false, length = 12) private String estado = "ABIERTA";
    @Column(name = "abierta_por", length = 100) private String abiertaPor;
    @Column(name = "cerrada_por", length = 100) private String cerradaPor;
    @Column(name = "fecha_apertura", nullable = false) private LocalDateTime fechaApertura;
    @Column(name = "fecha_cierre") private LocalDateTime fechaCierre;

    @PrePersist
    void onCreate() { if (fechaApertura == null) fechaApertura = LocalDateTime.now(); }
}
