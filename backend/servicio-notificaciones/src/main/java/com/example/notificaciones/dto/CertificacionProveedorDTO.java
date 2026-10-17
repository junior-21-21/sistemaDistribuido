package com.example.notificaciones.dto;

import java.time.LocalDateTime;

public record CertificacionProveedorDTO(
        String canal, String modo, String resultado, LocalDateTime certificadoEn) { }
