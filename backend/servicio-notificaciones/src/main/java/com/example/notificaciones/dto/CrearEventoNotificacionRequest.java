package com.example.notificaciones.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearEventoNotificacionRequest(
        @NotBlank @Size(max = 36) String eventoId,
        @NotBlank @Size(max = 40) String tipo,
        @NotNull Long estudianteId,
        @NotBlank @Size(max = 180) String asunto,
        @NotBlank @Size(max = 2000) String mensaje,
        Set<String> canales) {
}
