package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "atenciones_medicas", indexes = {
        @Index(name = "idx_atencion_ficha_fecha", columnList = "ficha_medica_id,fecha_atencion")
})
@Data
public class AtencionMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_medica_id", nullable = false)
    private FichaMedica fichaMedica;

    @Column(nullable = false, length = 200)
    private String motivo;

    @Column(nullable = false, length = 800)
    private String diagnostico;

    @Column(nullable = false, length = 800)
    private String tratamiento;

    @Column(name = "fecha_atencion", nullable = false, updatable = false)
    private LocalDateTime fechaAtencion;

    @Column(name = "registrado_por", nullable = false, length = 100)
    private String registradoPor;

    /** Se llamó al padre, madre o apoderado. */
    @Column(name = "apoderado_avisado", nullable = false)
    private boolean apoderadoAvisado;

    /** RETORNA_AULA, RETIRADO_APODERADO o TRASLADO_SALUD. */
    @Column(nullable = false, length = 25)
    private String destino = "RETORNA_AULA";

    /** Posta, centro de salud u Hospital Regional al que se trasladó al estudiante. */
    @Column(name = "establecimiento_salud", length = 150)
    private String establecimientoSalud;

    @Column(name = "cubierto_sis")
    private Boolean cubiertoSis;

    @PrePersist
    void onCreate() {
        if (fechaAtencion == null) fechaAtencion = LocalDateTime.now();
    }
}
