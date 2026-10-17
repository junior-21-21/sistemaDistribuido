package com.example.demo.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/internal/referencias")
public class InternalReferenciasController {
    private final JdbcTemplate jdbc;
    private final byte[] internalKey;

    public InternalReferenciasController(JdbcTemplate jdbc,
            @Value("${app.internal-api-key}") String internalKey) {
        this.jdbc = jdbc;
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping
    public Map<String, Object> referencias(@RequestHeader("X-Internal-Key") String suppliedKey) {
        requireInternalKey(suppliedKey);
        return Map.of(
                "notas", jdbc.query("SELECT id, estudiante_id, curso_id, docente_id, seccion_id FROM notas",
                        (rs, row) -> Map.of("id", rs.getLong("id"),
                                "estudianteId", rs.getLong("estudiante_id"),
                                "cursoId", rs.getLong("curso_id"),
                                "docenteId", rs.getLong("docente_id"),
                                "seccionId", rs.getLong("seccion_id"))),
                "asistencias", jdbc.query("SELECT id, estudiante_id, curso_id, docente_id, seccion_id, horario_id FROM asistencias",
                        (rs, row) -> Map.of("id", rs.getLong("id"),
                                "estudianteId", rs.getLong("estudiante_id"),
                                "cursoId", rs.getLong("curso_id"),
                                "docenteId", rs.getLong("docente_id"),
                                "seccionId", rs.getLong("seccion_id"),
                                "horarioId", rs.getLong("horario_id"))));
    }

    private void requireInternalKey(String suppliedKey) {
        if (!MessageDigest.isEqual(internalKey, suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Clave interna inválida");
        }
    }
}
