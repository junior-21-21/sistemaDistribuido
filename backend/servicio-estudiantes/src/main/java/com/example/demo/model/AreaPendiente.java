package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Área con competencias en C: RECUPERACION (acompañamiento al terminar el año) o SUBSANACION
 * (área que el estudiante promovido lleva y puede subsanar hasta diciembre del año siguiente).
 */
@Entity
@Table(name = "areas_pendientes")
@Data
public class AreaPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(name = "curso_nombre", nullable = false, length = 150)
    private String cursoNombre;

    @Column(name = "grado_origen", nullable = false, length = 60)
    private String gradoOrigen;

    @Column(name = "anio_origen", nullable = false)
    private Integer anioOrigen;

    /** PENDIENTE, SUBSANADA o NO_SUBSANADA. */
    @Column(nullable = false, length = 15)
    private String estado = "PENDIENTE";

    @Column(nullable = false, length = 15)
    private String tipo = "SUBSANACION";

    /** AD, A, B o C obtenida en la recuperación. */
    @Column(length = 2)
    private String calificacion;

    @Column(length = 300)
    private String observacion;

    @Column(name = "fecha_resultado")
    private LocalDate fechaResultado;

    @Column(name = "registrado_por", nullable = false, length = 100)
    private String registradoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
