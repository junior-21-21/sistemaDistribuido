package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Justificación de inasistencia, papeleta de salida o permiso de un estudiante.
 * El apoderado acude al auxiliar o al tutor; lo formal (permiso de varios días) pasa por
 * Secretaría y lo aprueba Dirección.
 */
@Entity
@Table(name = "solicitudes_permiso")
@Data
public class SolicitudPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    /** JUSTIFICACION, PAPELETA_SALIDA o PERMISO. */
    @Column(nullable = false, length = 25)
    private String tipo;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta", nullable = false)
    private LocalDate fechaHasta;

    @Column(name = "hora_salida")
    private LocalTime horaSalida;

    @Column(nullable = false, length = 400)
    private String motivo;

    @Column(name = "persona_recoge", length = 150)
    private String personaRecoge;

    @Column(name = "dni_persona_recoge", length = 8)
    private String dniPersonaRecoge;

    /** APODERADO (desde el portal) o PERSONAL (registrado en el colegio). */
    @Column(nullable = false, length = 12)
    private String origen;

    @Column(name = "solicitado_por", nullable = false, length = 100)
    private String solicitadoPor;

    /** PENDIENTE, APROBADA o RECHAZADA. */
    @Column(nullable = false, length = 12)
    private String estado = "PENDIENTE";

    @Column(name = "revisado_por", length = 100)
    private String revisadoPor;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "observacion_revision", length = 300)
    private String observacionRevision;

    /** Registros de asistencia que pasaron a JUSTIFICADO al aprobarse. */
    @Column(name = "asistencias_justificadas", nullable = false)
    private int asistenciasJustificadas;

    @Column(name = "salida_registrada_por", length = 100)
    private String salidaRegistradaPor;

    @Column(name = "fecha_salida")
    private LocalDateTime fechaSalida;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
