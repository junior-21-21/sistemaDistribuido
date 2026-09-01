package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "cursos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El codigo es obligatorio")
    @Column(unique = true, nullable = false, length = 15)
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false)
    private String nombre;

    @Column
    private Integer creditos;

    @Column
    private String tipo = "OBLIGATORIO";

    @NotNull(message = "Debe referenciar a un grado_id")
    @Column(name = "grado_id", nullable = false)
    private Long gradoId;

    @Column(nullable = false)
    private String estado = "ACTIVO";
}