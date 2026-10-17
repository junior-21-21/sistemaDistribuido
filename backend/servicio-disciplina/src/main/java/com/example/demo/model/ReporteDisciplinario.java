package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ReporteDisciplinario — Papeleta Digital.
 * Reemplaza las papeletas físicas de disciplina del colegio.
 * Tipos: TARDANZA, FALTA_LEVE, FALTA_GRAVE, MERITO, LLAMADA_ATENCION
 */
@Entity
@Table(name = "reportes_disciplinarios", indexes = {
    @Index(name = "idx_disc_estudiante", columnList = "estudianteId"),
    @Index(name = "idx_disc_docente", columnList = "docenteId"),
    @Index(name = "idx_disc_fecha", columnList = "fechaReporte")
})
@Data
public class ReporteDisciplinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long estudianteId;

    @Column
    private Long docenteId;

    @Column(nullable = false)
    private Long seccionId;

    /**
     * Tipo de incidencia:
     * TARDANZA         — Llegó tarde al colegio o al aula
     * FALTA_LEVE       — Comportamiento inadecuado leve
     * FALTA_GRAVE      — Comportamiento que amerita citación de apoderado
     * MERITO           — Reconocimiento positivo (logro académico, etc.)
     * LLAMADA_ATENCION — Advertencia verbal registrada
     */
    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 600)
    private String descripcion;

    /**
     * Estado del reporte:
     * EMITIDO       — Recién registrado
     * NOTIFICADO    — Apoderado notificado (WhatsApp/SMS)
     * ARCHIVADO     — Cerrado y archivado
     */
    @Column(nullable = false, length = 20)
    private String estado = "EMITIDO";

    @Column(name = "apoderado_notificado")
    private Boolean apoderadoNotificado = false;

    @Column(name = "fecha_reporte", nullable = false)
    private LocalDateTime fechaReporte;

    @Column(name = "fecha_notificacion")
    private LocalDateTime fechaNotificacion;

    @Column(name = "registrado_por", nullable = false, length = 100)
    private String registradoPor;

    @Column(length = 300)
    private String observaciones;

    // --- Protocolo de violencia escolar ---
    @Column(name = "codigo_siseve", length = 30)
    private String codigoSiseve;

    @Column(name = "fecha_siseve")
    private java.time.LocalDate fechaSiseve;

    @Column(name = "citacion_apoderado")
    private LocalDateTime citacionApoderado;

    @Column(name = "medidas_proteccion", length = 500)
    private String medidasProteccion;

    /** PSICOLOGIA, DEMUNA, CEM, POLICIA, FISCALIA o CENTRO_SALUD. */
    @Column(name = "derivacion_externa", length = 20)
    private String derivacionExterna;

    @Column(name = "fecha_derivacion")
    private java.time.LocalDate fechaDerivacion;

    @Column(name = "protocolo_actualizado_por", length = 100)
    private String protocoloActualizadoPor;

    @PrePersist
    protected void onCreate() {
        this.fechaReporte = LocalDateTime.now();
        if (this.estado == null) this.estado = "EMITIDO";
        if (this.apoderadoNotificado == null) this.apoderadoNotificado = false;
    }
}
