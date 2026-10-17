package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.model.AtencionMedica;

public record AtencionMedicaDTO(
        Long id,
        Long fichaMedicaId,
        Long estudianteId,
        String motivo,
        String diagnostico,
        String tratamiento,
        LocalDateTime fechaAtencion,
        String registradoPor,
        boolean apoderadoAvisado,
        String destino,
        String establecimientoSalud,
        Boolean cubiertoSis) {

    public static AtencionMedicaDTO from(AtencionMedica atencion) {
        return new AtencionMedicaDTO(
                atencion.getId(), atencion.getFichaMedica().getId(),
                atencion.getFichaMedica().getEstudianteId(), atencion.getMotivo(),
                atencion.getDiagnostico(), atencion.getTratamiento(),
                atencion.getFechaAtencion(), atencion.getRegistradoPor(),
                atencion.isApoderadoAvisado(), atencion.getDestino(),
                atencion.getEstablecimientoSalud(), atencion.getCubiertoSis());
    }
}
