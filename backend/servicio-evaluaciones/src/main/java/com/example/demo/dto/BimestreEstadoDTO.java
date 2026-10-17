package com.example.demo.dto;

import java.time.LocalDateTime;

public record BimestreEstadoDTO(
        Integer anio, String bimestre, String estado,
        String actualizadoPor, LocalDateTime actualizadoEn) { }
