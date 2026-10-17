package com.example.reportes.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ReconciliacionIntegridadService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliacionIntegridadService.class);

    private final WebClient estudiantesClient;
    private final WebClient cursosClient;
    private final WebClient evaluacionesClient;
    private final WebClient disciplinaClient;
    private final JdbcTemplate jdbc;
    private final String internalKey;

    public ReconciliacionIntegridadService(
            @Qualifier("estudiantesClient") WebClient estudiantesClient,
            @Qualifier("cursosClient") WebClient cursosClient,
            @Qualifier("evaluacionesClient") WebClient evaluacionesClient,
            @Qualifier("disciplinaClient") WebClient disciplinaClient,
            JdbcTemplate jdbc,
            @Value("${app.internal-api-key}") String internalKey) {
        this.estudiantesClient = estudiantesClient;
        this.cursosClient = cursosClient;
        this.evaluacionesClient = evaluacionesClient;
        this.disciplinaClient = disciplinaClient;
        this.jdbc = jdbc;
        this.internalKey = internalKey;
    }

    @Scheduled(initialDelayString = "${integridad.initial-delay-ms:90000}",
            fixedDelayString = "${integridad.interval-ms:21600000}")
    public void ejecutarProgramada() {
        try {
            ResultadoReconciliacion resultado = ejecutar();
            log.info("Reconciliación {} completada con {} incidencias",
                    resultado.ejecucionId(), resultado.incidenciasDetectadas());
        } catch (RuntimeException ex) {
            log.error("No se pudo completar la reconciliación de referencias", ex);
        }
    }

    @Transactional
    public ResultadoReconciliacion ejecutar() {
        JsonNode estudiantes = obtener(estudiantesClient);
        JsonNode cursos = obtener(cursosClient);
        JsonNode evaluaciones = obtener(evaluacionesClient);
        JsonNode disciplina = obtener(disciplinaClient);

        Set<Long> estudianteIds = idsDeFilas(estudiantes.path("estudiantes"));
        Set<Long> gradoIds = ids(cursos, "gradoIds");
        Set<Long> seccionIds = ids(cursos, "seccionIds");
        Set<Long> cursoIds = ids(cursos, "cursoIds");
        Set<Long> docenteIds = ids(cursos, "docenteIds");
        Set<Long> horarioIds = ids(cursos, "horarioIds");
        Set<Long> aulaIds = ids(cursos, "aulaIds");

        List<Incidencia> incidencias = new ArrayList<>();
        revisar(estudiantes.path("estudiantes"), "ESTUDIANTES", "estudiante", "gradoId", gradoIds, incidencias);
        revisar(estudiantes.path("estudiantes"), "ESTUDIANTES", "estudiante", "seccionId", seccionIds, incidencias);
        revisar(estudiantes.path("derivaciones"), "ESTUDIANTES", "derivacion", "estudianteId", estudianteIds, incidencias);
        revisar(estudiantes.path("derivaciones"), "ESTUDIANTES", "derivacion", "docenteId", docenteIds, incidencias);

        revisar(cursos.path("horarios"), "CURSOS", "horario", "seccionId", seccionIds, incidencias);
        revisar(cursos.path("horarios"), "CURSOS", "horario", "aulaId", aulaIds, incidencias);
        revisar(cursos.path("tickets"), "CURSOS", "ticket", "docenteId", docenteIds, incidencias);
        revisar(cursos.path("tickets"), "CURSOS", "ticket", "aulaId", aulaIds, incidencias);

        revisarAcademico(evaluaciones.path("notas"), "nota", estudianteIds, cursoIds,
                docenteIds, seccionIds, null, incidencias);
        revisarAcademico(evaluaciones.path("asistencias"), "asistencia", estudianteIds, cursoIds,
                docenteIds, seccionIds, horarioIds, incidencias);
        revisarAcademico(disciplina.path("reportes"), "reporte_disciplinario", estudianteIds, null,
                docenteIds, seccionIds, null, incidencias);

        UUID ejecucionId = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();
        jdbc.update("INSERT INTO ejecuciones_reconciliacion "
                        + "(id, ejecutada_en, incidencias_detectadas, estado, detalle) VALUES (?, ?, ?, 'COMPLETADA', ?)",
                ejecucionId, ahora, incidencias.size(), "Validación de referencias blandas entre servicios");
        jdbc.update("UPDATE incidencias_integridad SET estado='RESUELTA', resuelta_en=? WHERE estado='ABIERTA'", ahora);

        for (Incidencia incidencia : incidencias) {
            jdbc.update("""
                    INSERT INTO incidencias_integridad
                        (fingerprint, origen, entidad, registro_id, campo, valor_referenciado, problema,
                         estado, primera_deteccion, ultima_deteccion, ultima_ejecucion, resuelta_en)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'ABIERTA', ?, ?, ?, NULL)
                    ON CONFLICT (fingerprint) DO UPDATE SET
                        problema=EXCLUDED.problema, estado='ABIERTA', ultima_deteccion=EXCLUDED.ultima_deteccion,
                        ultima_ejecucion=EXCLUDED.ultima_ejecucion, resuelta_en=NULL
                    """, incidencia.fingerprint(), incidencia.origen(), incidencia.entidad(),
                    incidencia.registroId(), incidencia.campo(), incidencia.valorReferenciado(),
                    incidencia.problema(), ahora, ahora, ejecucionId);
        }
        return new ResultadoReconciliacion(ejecucionId.toString(), ahora, incidencias.size());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarIncidencias(String estado) {
        if (estado == null || estado.isBlank()) {
            return jdbc.queryForList("SELECT * FROM incidencias_integridad ORDER BY ultima_deteccion DESC");
        }
        return jdbc.queryForList("SELECT * FROM incidencias_integridad WHERE estado=? ORDER BY ultima_deteccion DESC",
                estado.toUpperCase());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarEjecuciones() {
        return jdbc.queryForList("SELECT * FROM ejecuciones_reconciliacion ORDER BY ejecutada_en DESC LIMIT 50");
    }

    private JsonNode obtener(WebClient client) {
        JsonNode result = client.get()
                .uri("/api/internal/referencias")
                .header("X-Internal-Key", internalKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(15));
        if (result == null) {
            throw new IllegalStateException("Un servicio devolvió una respuesta vacía durante la reconciliación");
        }
        return result;
    }

    private Set<Long> ids(JsonNode root, String field) {
        Set<Long> result = new HashSet<>();
        root.path(field).forEach(value -> result.add(value.asLong()));
        return result;
    }

    private Set<Long> idsDeFilas(JsonNode rows) {
        Set<Long> result = new HashSet<>();
        rows.forEach(row -> result.add(row.path("id").asLong()));
        return result;
    }

    private void revisarAcademico(JsonNode rows, String entidad, Set<Long> estudianteIds,
            Set<Long> cursoIds, Set<Long> docenteIds, Set<Long> seccionIds, Set<Long> horarioIds,
            List<Incidencia> incidencias) {
        revisar(rows, entidad.equals("reporte_disciplinario") ? "DISCIPLINA" : "EVALUACIONES",
                entidad, "estudianteId", estudianteIds, incidencias);
        if (cursoIds != null) revisar(rows, "EVALUACIONES", entidad, "cursoId", cursoIds, incidencias);
        revisar(rows, entidad.equals("reporte_disciplinario") ? "DISCIPLINA" : "EVALUACIONES",
                entidad, "docenteId", docenteIds, incidencias);
        revisar(rows, entidad.equals("reporte_disciplinario") ? "DISCIPLINA" : "EVALUACIONES",
                entidad, "seccionId", seccionIds, incidencias);
        if (horarioIds != null) revisar(rows, "EVALUACIONES", entidad, "horarioId", horarioIds, incidencias);
    }

    private void revisar(JsonNode rows, String origen, String entidad, String campo,
            Set<Long> validIds, List<Incidencia> incidencias) {
        rows.forEach(row -> {
            JsonNode valueNode = row.get(campo);
            if (valueNode == null || valueNode.isNull()) return;
            long value = valueNode.asLong();
            if (!validIds.contains(value)) {
                long id = row.path("id").asLong();
                String problema = campo + "=" + value + " no existe en su servicio propietario";
                incidencias.add(new Incidencia(fingerprint(origen, entidad, id, campo, value),
                        origen, entidad, id, campo, value, problema));
            }
        });
    }

    private String fingerprint(String origen, String entidad, long id, String campo, long value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (origen + '|' + entidad + '|' + id + '|' + campo + '|' + value)
                            .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private record Incidencia(String fingerprint, String origen, String entidad, long registroId,
            String campo, long valorReferenciado, String problema) {}

    public record ResultadoReconciliacion(String ejecucionId, LocalDateTime ejecutadaEn,
            int incidenciasDetectadas) {}
}
