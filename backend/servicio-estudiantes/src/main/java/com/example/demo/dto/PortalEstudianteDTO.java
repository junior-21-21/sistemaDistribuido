package com.example.demo.dto;

public record PortalEstudianteDTO(
        Long id,
        String nombre,
        String apellido,
        Long gradoId,
        Long seccionId,
        String parentesco,
        boolean permisoRetiro) {
}
