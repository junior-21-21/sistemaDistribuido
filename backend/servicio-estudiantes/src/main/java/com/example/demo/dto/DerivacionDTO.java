package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.model.DerivacionPsicologica;

public record DerivacionDTO(
        Long id,
        Long estudianteId,
        Long docenteDerivadorId,
        String motivo,
        String descripcion,
        String prioridad,
        String estado,
        LocalDateTime fechaDerivacion,
        LocalDateTime fechaAtencion,
        String notasPsicologo) {

    public static DerivacionDTO from(DerivacionPsicologica derivacion, boolean incluirNotas) {
        return new DerivacionDTO(
                derivacion.getId(), derivacion.getEstudianteId(), derivacion.getDocenteDerivadorId(),
                derivacion.getMotivo(), derivacion.getDescripcion(), derivacion.getPrioridad(),
                derivacion.getEstado(), derivacion.getFechaDerivacion(), derivacion.getFechaAtencion(),
                incluirNotas ? derivacion.getNotasPsicologo() : null);
    }
}
