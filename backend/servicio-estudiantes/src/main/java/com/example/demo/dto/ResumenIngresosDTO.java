package com.example.demo.dto;

import java.time.LocalDate;
import java.util.List;

public record ResumenIngresosDTO(
        LocalDate fecha,
        long estudiantesActivos,
        long ingresaron,
        long puntuales,
        long tardanzas,
        List<IngresoDTO> recientes) {
}
