package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** Evento inmutable de otorgamiento, denegación o revocación de consentimiento. */
@Entity
@Table(name = "consentimientos_datos")
@Data
public class ConsentimientoDatos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "estudiante_id", nullable = false) private Long estudianteId;
    @Column(name = "apoderado_id", nullable = false) private Long apoderadoId;
    @Column(nullable = false, length = 40) private String finalidad;
    @Column(nullable = false, length = 12) private String accion;
    @Column(name = "version_politica", nullable = false, length = 30) private String versionPolitica;
    @Column(length = 300) private String evidencia;
    @Column(name = "registrado_por", nullable = false, length = 100) private String registradoPor;
    @Column(name = "registrado_en", nullable = false) private LocalDateTime registradoEn;
}
