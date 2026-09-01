package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DocumentoAdjunto — Gestión Documental Digital.
 * Reemplaza el archivo físico. Permite a la secretaría adjuntar DNI,
 * partidas de nacimiento o resoluciones SIAGIE al legajo del alumno.
 */
@Entity
@Table(name = "documentos_adjuntos", indexes = {
    @Index(name = "idx_doc_estudiante", columnList = "estudianteId")
})
@Data
public class DocumentoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long estudianteId;

    @Column(nullable = false, length = 100)
    private String nombreArchivo; // ej. "DNI_Juan.pdf"

    @Column(nullable = false, length = 50)
    private String tipoDocumento; // DNI, PARTIDA_NACIMIENTO, RESOLUCION_SIAGIE, CERTIFICADO_MEDICO

    @Column(nullable = false, length = 50)
    private String contentType; // application/pdf, image/jpeg

    @Column(nullable = false)
    private Long tamanoBytes;

    // Almacenamos el archivo directamente en BD para simplificar la distribución 
    // y evitar problemas de permisos de escritura en disco en la etapa de desarrollo.
    // (En un entorno cloud enterprise esto se migraría a un bucket S3)
    @Lob
    @Column(nullable = false)
    private byte[] datos;

    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "subido_por", length = 100)
    private String subidoPor; // username (ej. secretaria1)

    @PrePersist
    protected void onCreate() {
        this.fechaSubida = LocalDateTime.now();
    }
}
