package com.example.demo.dto;

import java.time.LocalDateTime;

public record ConsentimientoDatosDTO(
        Long id, Long estudianteId, Long apoderadoId, String finalidad, String accion,
        String versionPolitica, String evidencia, String registradoPor, LocalDateTime registradoEn) { }
