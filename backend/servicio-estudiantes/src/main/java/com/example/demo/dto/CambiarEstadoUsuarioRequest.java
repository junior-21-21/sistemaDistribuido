package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoUsuarioRequest(@NotBlank String estado) {
}
