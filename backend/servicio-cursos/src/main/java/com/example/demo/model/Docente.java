package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "docentes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Docente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Column(nullable = false)
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Column(unique = true, nullable = false, length = 8)
    private String dni;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email no valido")
    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String especialidad;

    @Column(length = 100)
    private String username;

    @Min(value = 1, message = "Las horas contratadas deben ser mayores a cero")
    @Column(name = "horas_semanales_contratadas", nullable = false)
    private Integer horasSemanalesContratadas = 30;

    @Column(nullable = false)
    private String estado = "ACTIVO";

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
