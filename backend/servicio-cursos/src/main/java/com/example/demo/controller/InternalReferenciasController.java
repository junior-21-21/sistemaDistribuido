package com.example.demo.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
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
                "gradoIds", ids("grados"),
                "seccionIds", ids("secciones"),
                "cursoIds", ids("cursos"),
                "docenteIds", ids("docentes"),
                "horarioIds", ids("horarios"),
                "aulaIds", ids("aulas"),
                "horarios", jdbc.query(
                        "SELECT id, seccion_id, aula_id FROM horarios",
                        (rs, row) -> Map.of("id", rs.getLong("id"),
                                "seccionId", rs.getLong("seccion_id"),
                                "aulaId", rs.getLong("aula_id"))),
                "tickets", jdbc.query(
                        "SELECT id, docente_id, aula_id FROM tickets_mantenimiento",
                        (rs, row) -> Map.of("id", rs.getLong("id"),
                                "docenteId", rs.getLong("docente_id"),
                                "aulaId", rs.getLong("aula_id"))));
    }

    private List<Long> ids(String table) {
        return jdbc.query("SELECT id FROM " + table, (rs, row) -> rs.getLong(1));
    }

    private void requireInternalKey(String suppliedKey) {
        if (!MessageDigest.isEqual(internalKey, suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Clave interna inválida");
        }
    }
}
