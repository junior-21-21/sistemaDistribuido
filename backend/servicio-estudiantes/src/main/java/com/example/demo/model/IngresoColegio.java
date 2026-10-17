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

/** Llegada de un estudiante al colegio, registrada en portería con su DNI. */
@Entity
@Table(name = "ingresos_colegio")
@Data
public class IngresoColegio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false, length = 10)
    private String turno;

    /** PUNTUAL o TARDANZA. */
    @Column(nullable = false, length = 12)
    private String estado;

    /** ESCANER (lector de código de barras), CAMARA o MANUAL. */
    @Column(nullable = false, length = 12)
    private String metodo;

    @Column(name = "registrado_por", nullable = false, length = 100)
    private String registradoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
