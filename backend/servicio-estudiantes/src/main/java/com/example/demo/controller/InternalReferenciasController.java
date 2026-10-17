package com.example.demo.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.LinkedHashMap;
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

    public InternalReferenciasController(
            JdbcTemplate jdbc,
            @Value("${app.internal-api-key}") String internalKey) {
        this.jdbc = jdbc;
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping
    public Map<String, Object> referencias(@RequestHeader("X-Internal-Key") String suppliedKey) {
        requireInternalKey(suppliedKey);
        List<Map<String, Object>> estudiantes = jdbc.query(
                "SELECT id, grado_id, seccion_id FROM estudiantes",
                (rs, row) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getLong("id"));
                    item.put("gradoId", rs.getObject("grado_id", Long.class));
                    item.put("seccionId", rs.getObject("seccion_id", Long.class));
                    return item;
                });
        List<Map<String, Object>> derivaciones = jdbc.query(
                "SELECT id, estudiante_id, docente_derivador_id FROM derivaciones_psicologicas",
                (rs, row) -> Map.of(
                        "id", rs.getLong("id"),
                        "estudianteId", rs.getLong("estudiante_id"),
                        "docenteId", rs.getLong("docente_derivador_id")));
        return Map.of("estudiantes", estudiantes, "derivaciones", derivaciones);
    }

    private void requireInternalKey(String suppliedKey) {
        if (!MessageDigest.isEqual(internalKey, suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Clave interna inválida");
        }
    }
}
