package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Grado escolar (Ej: 1ro, 2do, 3ro...)
 * Pertenece a un Nivel educativo.
 */
@Entity
@Table(name = "grados")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del grado es obligatorio")
    @Column(nullable = false, length = 30)
    private String nombre; // "1ro", "2do", "3ro"

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_id", nullable = false)
    private Nivel nivel;

    @Column(nullable = false)
    private Boolean activo = true;
}
