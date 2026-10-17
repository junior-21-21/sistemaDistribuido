package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.example.demo.model.Usuario;

public record UsuarioDTO(
        Long id,
        String username,
        String nombre,
        String apellido,
        String email,
        String rol,
        Set<String> roles,
        String estado,
        LocalDateTime ultimoAcceso,
        boolean debeCambiarClave,
        Long apoderadoId) {

    public static UsuarioDTO from(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getRol(),
                Set.copyOf(usuario.getRoles()),
                usuario.getEstado(),
                usuario.getUltimoAcceso(),
                Boolean.TRUE.equals(usuario.getDebeCambiarClave()),
                usuario.getApoderado() == null ? null : usuario.getApoderado().getId());
    }
}
