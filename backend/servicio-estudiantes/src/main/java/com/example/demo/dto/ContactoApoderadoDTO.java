package com.example.demo.dto;

public record ContactoApoderadoDTO(
        Long apoderadoId,
        Long estudianteId,
        String nombre,
        String parentesco,
        String email,
        String telefono) {
}
