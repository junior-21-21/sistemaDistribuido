package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Resultado de registrar o consultar un ingreso al colegio. */
public record IngresoDTO(
        Long id,
        Long estudianteId,
        String nombre,
        String apellido,
        String dniFinal,
        Long gradoId,
        Long seccionId,
        LocalDate fecha,
        LocalTime hora,
        String turno,
        String estado,
        long minutosTarde,
        String metodo,
        boolean yaRegistrado) {
}
