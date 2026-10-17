package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "estudiantes")
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Estudiante {

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

    @Email(message = "Email no valido")
    @Column(unique = true)
    private String email;

    @Column
    private String telefono;

    @Column(name = "grado_id")
    private Long gradoId;

    @Column(name = "seccion_id")
    private Long seccionId;

    @Column(nullable = false)
    private String estado = "ACTIVO";

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
