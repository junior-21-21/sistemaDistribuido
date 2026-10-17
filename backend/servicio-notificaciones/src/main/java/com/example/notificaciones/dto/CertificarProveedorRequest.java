package com.example.notificaciones.dto;

import jakarta.validation.constraints.NotBlank;

public record CertificarProveedorRequest(
        @NotBlank String canal,
        @NotBlank String destinatario) { }
