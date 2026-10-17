package com.example.demo.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{4,40}$", message = "El usuario debe tener entre 4 y 40 caracteres válidos") String username,
        @NotBlank
        @Size(min = 4, max = 72, message = "La clave debe tener entre 4 y 72 caracteres")
        String clave,
        @NotBlank @Size(max = 80) String nombre,
        @NotBlank @Size(max = 120) String apellido,
        @NotBlank @Email @Size(max = 160) String email,
        String rol,
        Set<String> roles,
        Long apoderadoId) {
}
