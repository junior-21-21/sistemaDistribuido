package com.example.demo.dto;

import java.util.List;
import java.util.Map;

public record PortalResumenDTO(
        PortalEstudianteDTO estudiante,
        List<DocumentoDTO> documentos,
        List<Map<String, Object>> notas,
        List<Map<String, Object>> asistencias) {
}
