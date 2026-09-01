package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Aula — Ambiente físico del colegio.
 * Control de doble turno: MANANA, TARDE, AMBOS.
 * capacidadMaxima impide hacinamiento en matrículas.
 */
@Entity
@Table(name = "aulas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del aula es obligatorio")
    @Column(nullable = false, length = 50)
    private String nombre; // Ej: "Aula 101"

    @NotBlank(message = "El pabellón es obligatorio")
    @Column(nullable = false, length = 50)
    private String pabellon; // Ej: "Pabellón A"

    @NotNull
    @Min(value = 1, message = "La capacidad debe ser al menos 1")
    @Column(nullable = false)
    private Integer capacidadMaxima; // Máx. estudiantes por aula

    /**
     * Disponibilidad de turno:
     * MANANA — Solo turno mañana
     * TARDE  — Solo turno tarde
     * AMBOS  — Disponible para ambos turnos (más común en colegios públicos)
     */
    @Column(nullable = false, length = 10)
    private String turnoDisponible = "AMBOS";

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(length = 200)
    private String observaciones;
}
