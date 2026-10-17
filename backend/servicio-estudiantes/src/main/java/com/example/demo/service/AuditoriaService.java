package com.example.demo.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.audit.RevisionInfo;
import com.example.demo.dto.AuditoriaDTO;
import com.example.demo.dto.PageResponse;
import com.example.demo.model.Apoderado;
import com.example.demo.model.Documento;
import com.example.demo.model.Estudiante;

import jakarta.persistence.EntityManager;

@Service
@Transactional(readOnly = true)
public class AuditoriaService {

    private static final Map<String, Class<?>> ENTIDADES = Map.of(
            "ESTUDIANTE", Estudiante.class,
            "APODERADO", Apoderado.class,
            "DOCUMENTO", Documento.class);

    private final EntityManager entityManager;

    public AuditoriaService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public PageResponse<AuditoriaDTO> buscar(
            String entidad, Long entidadId, String usuario,
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        String codigo = entidad == null ? "" : entidad.trim().toUpperCase(Locale.ROOT);
        Class<?> entityClass = ENTIDADES.get(codigo);
        if (entityClass == null) {
            throw new IllegalArgumentException("Entidad auditable inválida. Use ESTUDIANTE, APODERADO o DOCUMENTO");
        }

        AuditReader reader = AuditReaderFactory.get(entityManager);
        AuditQuery query = filtros(reader.createQuery().forRevisionsOfEntity(entityClass, false, true),
                entidadId, usuario, desde, hasta);
        query.addOrder(AuditEntity.revisionNumber().desc())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(Math.min(pageable.getPageSize(), 100));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<AuditoriaDTO> content = rows.stream().map(row -> toDto(codigo, row)).toList();

        AuditQuery countQuery = filtros(reader.createQuery().forRevisionsOfEntity(entityClass, false, true),
                entidadId, usuario, desde, hasta);
        countQuery.addProjection(AuditEntity.revisionNumber().count());
        long total = ((Number) countQuery.getSingleResult()).longValue();
        int size = Math.min(pageable.getPageSize(), 100);
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, pageable.getPageNumber(), size, total, totalPages,
                pageable.getPageNumber() == 0, pageable.getPageNumber() + 1 >= totalPages);
    }

    private AuditQuery filtros(AuditQuery query, Long entidadId, String usuario,
                               LocalDateTime desde, LocalDateTime hasta) {
        if (entidadId != null) query.add(AuditEntity.id().eq(entidadId));
        if (usuario != null && !usuario.isBlank()) {
            query.add(AuditEntity.revisionProperty("username").eq(usuario.trim()));
        }
        ZoneId zone = ZoneId.systemDefault();
        if (desde != null) query.add(AuditEntity.revisionProperty("timestamp")
                .ge(desde.atZone(zone).toInstant().toEpochMilli()));
        if (hasta != null) query.add(AuditEntity.revisionProperty("timestamp")
                .le(hasta.atZone(zone).toInstant().toEpochMilli()));
        return query;
    }

    private AuditoriaDTO toDto(String entidad, Object[] row) {
        Object snapshot = row[0];
        RevisionInfo revision = (RevisionInfo) row[1];
        String accion = row[2].toString();
        return new AuditoriaDTO(revision.getRevision(), Instant.ofEpochMilli(revision.getTimestamp()),
                accion, entidad, id(snapshot), revision.getUsername(), revision.getCorrelationId(), valores(snapshot));
    }

    private Long id(Object snapshot) {
        if (snapshot instanceof Estudiante item) return item.getId();
        if (snapshot instanceof Apoderado item) return item.getId();
        if (snapshot instanceof Documento item) return item.getId();
        return null;
    }

    private Map<String, Object> valores(Object snapshot) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (snapshot instanceof Estudiante item) {
            values.put("nombre", item.getNombre());
            values.put("apellido", item.getApellido());
            values.put("dni", item.getDni());
            values.put("email", item.getEmail());
            values.put("gradoId", item.getGradoId());
            values.put("seccionId", item.getSeccionId());
            values.put("estado", item.getEstado());
        } else if (snapshot instanceof Apoderado item) {
            values.put("nombre", item.getNombre());
            values.put("apellido", item.getApellido());
            values.put("dni", item.getDni());
            values.put("email", item.getEmail());
            values.put("telefono", item.getTelefono());
        } else if (snapshot instanceof Documento item) {
            values.put("nombreArchivo", item.getNombreArchivo());
            values.put("tipoDocumento", item.getTipoDocumento());
            values.put("fechaSubida", item.getFechaSubida());
            values.put("estudianteId", item.getEstudiante() == null ? null : item.getEstudiante().getId());
        }
        return values;
    }
}
