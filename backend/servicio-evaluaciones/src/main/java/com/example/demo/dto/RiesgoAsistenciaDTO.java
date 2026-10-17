package com.example.demo.dto;

import java.time.LocalDate;

public record RiesgoAsistenciaDTO(
        Long estudianteId,
        Long seccionId,
        long faltasRecientes,
        LocalDate ultimaFalta) {
}
