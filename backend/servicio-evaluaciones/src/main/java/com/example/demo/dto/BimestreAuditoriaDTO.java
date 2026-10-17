package com.example.demo.dto;

import java.time.LocalDateTime;

public record BimestreAuditoriaDTO(
        String accion, String motivo, String actor, LocalDateTime ocurridoEn) { }
