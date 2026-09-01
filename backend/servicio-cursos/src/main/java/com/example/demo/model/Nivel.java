package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Nivel educativo: PRIMARIA o SECUNDARIA.
 */
@Entity
@Table(name = "niveles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Nivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del nivel es obligatorio")
    @Column(unique = true, nullable = false, length = 30)
    private String nombre; // PRIMARIA, SECUNDARIA

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;
}
