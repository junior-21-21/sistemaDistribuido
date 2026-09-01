package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Sección de estudiantes (Ej: 3ro "A", 3ro "B")
 * Pertenece a un Grado. Tiene capacidad máxima y turno asignado.
 */
@Entity
@Table(name = "secciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la sección es obligatorio")
    @Column(nullable = false, length = 10)
    private String nombre; // "A", "B", "C"

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grado_id", nullable = false)
    private Grado grado;

    @NotNull
    @Column(nullable = false, length = 10)
    private String turno; // MANANA, TARDE

    @Min(value = 1)
    @Column(nullable = false)
    private Integer capacidadMaxima = 35;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "anio_escolar", nullable = false)
    private Integer anioEscolar;

    /** Nombre del tutor asignado a esta sección */
    @Column(name = "docente_tutor_id")
    private Long docenteTutorId;
}
