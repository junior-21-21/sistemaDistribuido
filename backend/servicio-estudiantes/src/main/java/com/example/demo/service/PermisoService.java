package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.demo.dto.CrearPermisoRequest;
import com.example.demo.dto.PermisoDTO;
import com.example.demo.model.ApoderadoEstudiante;
import com.example.demo.model.Estudiante;
import com.example.demo.model.SolicitudPermiso;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ApoderadoEstudianteRepository;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.repository.SolicitudPermisoRepository;
import com.example.demo.repository.UsuarioRepository;

/**
 * Justificaciones de inasistencia, papeletas de salida y permisos.
 * <ul>
 *   <li>Justificación y papeleta de salida: las aprueba el auxiliar, la coordinación de TOE o Dirección.</li>
 *   <li>Permiso formal (varios días): lo registra Secretaría (mesa de partes) y lo aprueba Dirección.</li>
 * </ul>
 * Al aprobar una justificación o un permiso, las faltas y tardanzas de esas fechas pasan a JUSTIFICADO.
 */
@Service
@Transactional(readOnly = true)
public class PermisoService {

    public static final String JUSTIFICACION = "JUSTIFICACION";
    public static final String PAPELETA_SALIDA = "PAPELETA_SALIDA";
    public static final String PERMISO = "PERMISO";

    private static final Set<String> APRUEBAN_DIARIO = Set.of("AUXILIAR", "RESPONSABLE_CONVIVENCIA", "DIRECTOR", "SUBDIRECTOR");
    private static final Set<String> APRUEBAN_FORMAL = Set.of("DIRECTOR");
    private static final Set<String> REGISTRAN_FORMAL = Set.of("SECRETARIA", "DIRECTOR", "SUBDIRECTOR");
    private static final Set<String> CONTROLAN_SALIDA = Set.of("PORTERO", "AUXILIAR", "RESPONSABLE_CONVIVENCIA");

    private final SolicitudPermisoRepository repository;
    private final EstudianteRepository estudiantes;
    private final UsuarioRepository usuarios;
    private final ApoderadoEstudianteRepository vinculos;
    private final NotificacionClient notificacionClient;
    private final RestClient evaluacionesClient;
    private final String internalApiKey;

    public PermisoService(
            SolicitudPermisoRepository repository,
            EstudianteRepository estudiantes,
            UsuarioRepository usuarios,
            ApoderadoEstudianteRepository vinculos,
            NotificacionClient notificacionClient,
            RestClient.Builder restClientBuilder,
            @Value("${services.evaluaciones-url:http://servicio-evaluaciones:8084}") String evaluacionesUrl,
            @Value("${app.internal-api-key}") String internalApiKey) {
        this.repository = repository;
        this.estudiantes = estudiantes;
        this.usuarios = usuarios;
        this.vinculos = vinculos;
        this.notificacionClient = notificacionClient;
        this.evaluacionesClient = restClientBuilder.baseUrl(evaluacionesUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public List<PermisoDTO> listar(String estado, LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde == null ? LocalDate.now().minusDays(30) : desde;
        LocalDate fin = hasta == null ? LocalDate.now().plusDays(60) : hasta;
        String filtro = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase();
        return aDto(repository.buscar(filtro, inicio, fin));
    }

    /** Papeletas de salida aprobadas para hoy: la lista que revisa la portería. */
    public List<PermisoDTO> salidasDeHoy() {
        return aDto(repository.findByTipoAndEstadoAndFechaDesdeOrderByHoraSalidaAsc(
                PAPELETA_SALIDA, "APROBADA", LocalDate.now()));
    }

    public long pendientes() {
        return repository.countByEstado("PENDIENTE");
    }

    @Transactional
    public PermisoDTO registrarPorPersonal(CrearPermisoRequest request, String actor, Set<String> roles) {
        String tipo = request.tipo();
        if (PERMISO.equals(tipo) && roles.stream().noneMatch(REGISTRAN_FORMAL::contains)) {
            throw new AccessDeniedException("Los permisos formales se presentan en Secretaría (mesa de partes)");
        }
        if (roles.stream().noneMatch(r -> REGISTRAN_FORMAL.contains(r) || APRUEBAN_DIARIO.contains(r) || "DOCENTE".equals(r))) {
            throw new AccessDeniedException("Tu rol no registra justificaciones ni permisos");
        }
        SolicitudPermiso solicitud = nueva(request, "PERSONAL", actor);
        solicitud = repository.save(solicitud);
        if (Boolean.TRUE.equals(request.aprobarAhora()) && puedeAprobar(tipo, roles)) {
            return revisar(solicitud.getId(), true, null, actor, roles);
        }
        return aDto(List.of(solicitud)).get(0);
    }

    @Transactional
    public PermisoDTO registrarPorApoderado(CrearPermisoRequest request, String username) {
        validarVinculo(username, request.estudianteId());
        SolicitudPermiso solicitud = nueva(request, "APODERADO", username);
        return aDto(List.of(repository.save(solicitud))).get(0);
    }

    public List<PermisoDTO> listarDeApoderado(String username) {
        List<Long> hijos = vinculosDe(username).stream().map(v -> v.getEstudiante().getId()).toList();
        return hijos.isEmpty() ? List.of() : aDto(repository.findByEstudianteIdInOrderByCreadoEnDesc(hijos));
    }

    @Transactional
    public PermisoDTO revisar(Long id, boolean aprobar, String observacion, String actor, Set<String> roles) {
        SolicitudPermiso solicitud = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new IllegalArgumentException("La solicitud ya fue revisada");
        }
        if (!puedeAprobar(solicitud.getTipo(), roles)) {
            throw new AccessDeniedException(PERMISO.equals(solicitud.getTipo())
                    ? "Los permisos formales los aprueba Dirección"
                    : "Las justificaciones y papeletas las aprueba el auxiliar, TOE o Dirección");
        }
        String nota = observacion == null ? null : observacion.trim();
        if (!aprobar && (nota == null || nota.isEmpty())) {
            throw new IllegalArgumentException("Indica el motivo para no aprobar la solicitud");
        }
        solicitud.setEstado(aprobar ? "APROBADA" : "RECHAZADA");
        solicitud.setRevisadoPor(actor);
        solicitud.setFechaRevision(LocalDateTime.now());
        solicitud.setObservacionRevision(nota);
        if (aprobar && !PAPELETA_SALIDA.equals(solicitud.getTipo())) {
            solicitud.setAsistenciasJustificadas(justificarAsistencias(solicitud));
        }
        solicitud = repository.save(solicitud);
        notificacionClient.permiso(solicitud.getId(), solicitud.getEstudianteId(), solicitud.getTipo(),
                solicitud.getEstado(), nota);
        return aDto(List.of(solicitud)).get(0);
    }

    /** La portería confirma que el estudiante salió con la persona autorizada. */
    @Transactional
    public PermisoDTO registrarSalida(Long id, String actor, Set<String> roles) {
        if (roles.stream().noneMatch(CONTROLAN_SALIDA::contains)) {
            throw new AccessDeniedException("La salida la registra la portería o el auxiliar");
        }
        SolicitudPermiso solicitud = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Papeleta no encontrada"));
        if (!PAPELETA_SALIDA.equals(solicitud.getTipo()) || !"APROBADA".equals(solicitud.getEstado())) {
            throw new IllegalArgumentException("Solo se registra la salida de una papeleta aprobada");
        }
        if (!LocalDate.now().equals(solicitud.getFechaDesde())) {
            throw new IllegalArgumentException("La papeleta no es para hoy");
        }
        if (solicitud.getFechaSalida() != null) {
            throw new IllegalArgumentException("La salida ya fue registrada");
        }
        solicitud.setSalidaRegistradaPor(actor);
        solicitud.setFechaSalida(LocalDateTime.now());
        return aDto(List.of(repository.save(solicitud))).get(0);
    }

    private boolean puedeAprobar(String tipo, Set<String> roles) {
        Set<String> aprueban = PERMISO.equals(tipo) ? APRUEBAN_FORMAL : APRUEBAN_DIARIO;
        return roles.stream().anyMatch(aprueban::contains);
    }

    private SolicitudPermiso nueva(CrearPermisoRequest request, String origen, String actor) {
        Estudiante estudiante = estudiantes.findById(request.estudianteId())
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        if (!"ACTIVO".equals(estudiante.getEstado())) {
            throw new IllegalArgumentException("El estudiante no está activo");
        }
        LocalDate desde = request.fechaDesde();
        LocalDate hasta = request.fechaHasta() == null ? desde : request.fechaHasta();
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        if (desde.isBefore(LocalDate.now().minusDays(30))) {
            throw new IllegalArgumentException("Solo se justifican inasistencias de los últimos 30 días");
        }
        SolicitudPermiso solicitud = new SolicitudPermiso();
        solicitud.setEstudianteId(estudiante.getId());
        solicitud.setTipo(request.tipo());
        solicitud.setMotivo(request.motivo().trim());
        solicitud.setOrigen(origen);
        solicitud.setSolicitadoPor(actor);
        switch (request.tipo()) {
            case JUSTIFICACION -> {
                if (desde.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException("Una justificación es de días ya pasados; para días futuros pide un permiso");
                }
            }
            case PAPELETA_SALIDA -> {
                if (request.horaSalida() == null) {
                    throw new IllegalArgumentException("Indica la hora de salida");
                }
                if (desde.isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("La papeleta de salida es para hoy o un día próximo");
                }
                if (request.personaRecoge() == null || request.personaRecoge().isBlank()) {
                    throw new IllegalArgumentException("Indica quién recoge al estudiante");
                }
                hasta = desde;
                solicitud.setHoraSalida(request.horaSalida());
                solicitud.setPersonaRecoge(request.personaRecoge().trim());
                solicitud.setDniPersonaRecoge(blankToNull(request.dniPersonaRecoge()));
            }
            default -> {
                if (hasta.isAfter(desde.plusDays(60))) {
                    throw new IllegalArgumentException("Un permiso no puede superar 60 días");
                }
            }
        }
        solicitud.setFechaDesde(desde);
        solicitud.setFechaHasta(hasta);
        return solicitud;
    }

    private int justificarAsistencias(SolicitudPermiso solicitud) {
        try {
            Map<?, ?> respuesta = evaluacionesClient.post()
                    .uri("/api/internal/asistencias/justificar")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .body(Map.of(
                            "estudianteId", solicitud.getEstudianteId(),
                            "desde", solicitud.getFechaDesde().toString(),
                            "hasta", solicitud.getFechaHasta().toString(),
                            "motivo", solicitud.getMotivo()))
                    .retrieve()
                    .body(Map.class);
            Object total = respuesta == null ? null : respuesta.get("justificadas");
            return total instanceof Number numero ? numero.intValue() : 0;
        } catch (RestClientException ex) {
            throw new IllegalArgumentException(
                    "No se pudo actualizar la asistencia en este momento; intenta aprobar de nuevo en unos segundos");
        }
    }

    private void validarVinculo(String username, Long estudianteId) {
        boolean propio = vinculosDe(username).stream()
                .anyMatch(v -> v.getEstudiante().getId().equals(estudianteId));
        if (!propio) {
            throw new AccessDeniedException("El estudiante no está asociado a este apoderado");
        }
    }

    private List<ApoderadoEstudiante> vinculosDe(String username) {
        Usuario usuario = usuarios.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (usuario.getApoderado() == null) {
            throw new AccessDeniedException("La cuenta no está vinculada a un apoderado");
        }
        return vinculos.findByApoderadoId(usuario.getApoderado().getId());
    }

    private List<PermisoDTO> aDto(Collection<SolicitudPermiso> solicitudes) {
        Set<Long> ids = solicitudes.stream().map(SolicitudPermiso::getEstudianteId).collect(Collectors.toSet());
        Map<Long, Estudiante> porId = estudiantes.findAllById(ids).stream()
                .collect(Collectors.toMap(Estudiante::getId, Function.identity()));
        return solicitudes.stream().map(s -> {
            Estudiante e = porId.get(s.getEstudianteId());
            return new PermisoDTO(s.getId(), s.getEstudianteId(),
                    e == null ? "Estudiante #" + s.getEstudianteId() : e.getApellido() + ", " + e.getNombre(),
                    e == null ? null : e.getDni(), e == null ? null : e.getSeccionId(),
                    s.getTipo(), s.getFechaDesde(), s.getFechaHasta(), s.getHoraSalida(), s.getMotivo(),
                    s.getPersonaRecoge(), s.getDniPersonaRecoge(), s.getOrigen(), s.getSolicitadoPor(),
                    s.getEstado(), s.getRevisadoPor(), s.getFechaRevision(), s.getObservacionRevision(),
                    s.getAsistenciasJustificadas(), s.getSalidaRegistradaPor(), s.getFechaSalida(), s.getCreadoEn());
        }).toList();
    }

    private static String blankToNull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
