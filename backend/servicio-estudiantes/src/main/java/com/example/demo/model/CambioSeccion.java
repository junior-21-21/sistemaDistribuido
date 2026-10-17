package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** Traslado interno de un estudiante a otra sección del mismo grado. */
@Entity
@Table(name = "cambios_seccion")
@Data
public class CambioSeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(name = "anio_escolar", nullable = false)
    private Integer anioEscolar;

    @Column(name = "seccion_origen_id")
    private Long seccionOrigenId;

    @Column(name = "seccion_destino_id", nullable = false)
    private Long seccionDestinoId;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(name = "registrado_por", nullable = false, length = 100)
    private String registradoPor;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
