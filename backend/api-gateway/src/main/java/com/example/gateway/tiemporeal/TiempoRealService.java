package com.example.gateway.tiemporeal;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Bus en memoria de cambios. El gateway es la única puerta de escritura del sistema, por lo
 * que observar aquí las respuestas 2xx de POST/PUT/PATCH/DELETE cubre todos los microservicios.
 */
@Service
public class TiempoRealService {

    private static final Set<String> ACADEMICO = Set.of("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA");

    /** Qué roles pueden enterarse de que un módulo cambió (alineado con SecurityConfig). */
    private static final Map<String, Set<String>> AUDIENCIA = Map.ofEntries(
            Map.entry("notas", Set.of("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "APODERADO")),
            Map.entry("bimestres", Set.of("DIRECTOR", "SUBDIRECTOR", "DOCENTE")),
            Map.entry("recuperacion", Set.of("SECRETARIA", "DIRECTOR", "SUBDIRECTOR", "DOCENTE")),
            Map.entry("ingresos", Set.of("AUXILIAR", "RESPONSABLE_CONVIVENCIA", "SECRETARIA", "DIRECTOR", "SUBDIRECTOR", "DOCENTE", "PORTERO")),
            Map.entry("permisos", Set.of("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR",
                    "RESPONSABLE_CONVIVENCIA", "PORTERO", "APODERADO")),
            Map.entry("nominas", Set.of("DIRECTOR", "SUBDIRECTOR", "SECRETARIA")),
            Map.entry("asistencias", Set.of("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA", "APODERADO")),
            Map.entry("reportes-disciplinarios", Set.of("DIRECTOR", "SUBDIRECTOR", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA")),
            Map.entry("disciplina", Set.of("DIRECTOR", "SUBDIRECTOR", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA")),
            Map.entry("tickets", Set.of("DOCENTE", "MANTENIMIENTO", "DIRECTOR", "SUBDIRECTOR")),
            Map.entry("derivaciones", Set.of("DOCENTE", "PSICOLOGO", "RESPONSABLE_CONVIVENCIA")),
            Map.entry("fichas-medicas", Set.of("ENFERMERA", "PRIMEROS_AUXILIOS")),
            Map.entry("atenciones-medicas", Set.of("ENFERMERA", "PRIMEROS_AUXILIOS")),
            Map.entry("estudiantes", Set.of("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR")),
            Map.entry("apoderados", Set.of("DIRECTOR", "SUBDIRECTOR", "SECRETARIA")),
            Map.entry("consentimientos", Set.of("DIRECTOR", "SUBDIRECTOR", "SECRETARIA")),
            Map.entry("documentos", Set.of("DIRECTOR", "SUBDIRECTOR", "SECRETARIA")),
            Map.entry("cursos", ACADEMICO),
            Map.entry("docentes", ACADEMICO),
            Map.entry("secciones", ACADEMICO),
            Map.entry("grados", ACADEMICO),
            Map.entry("niveles", ACADEMICO),
            Map.entry("periodos", ACADEMICO),
            Map.entry("horarios", ACADEMICO),
            Map.entry("asignaciones-academicas", ACADEMICO),
            Map.entry("aulas", Set.of("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA", "MANTENIMIENTO")),
            Map.entry("usuarios", Set.of("DIRECTOR")),
            Map.entry("notificaciones", Set.of("DIRECTOR", "SUBDIRECTOR", "RESPONSABLE_CONVIVENCIA")),
            Map.entry("integridad", Set.of("DIRECTOR")),
            Map.entry("actividades", Set.of("DIRECTOR", "SUBDIRECTOR", "RESPONSABLE_CONVIVENCIA")),
            Map.entry("institucion", Set.of("DIRECTOR", "SUBDIRECTOR", "SECRETARIA", "DOCENTE", "AUXILIAR", "RESPONSABLE_CONVIVENCIA",
                    "PRIMEROS_AUXILIOS", "PSICOLOGO", "ENFERMERA", "MANTENIMIENTO", "PORTERO", "APODERADO")));

    private static final Map<String, String> NOMBRE_MODULO = Map.ofEntries(
            Map.entry("notas", "notas"),
            Map.entry("bimestres", "el estado de un bimestre"),
            Map.entry("asistencias", "asistencia"),
            Map.entry("ingresos", "un ingreso al colegio"),
            Map.entry("permisos", "una justificación o permiso"),
            Map.entry("nominas", "una nómina de matrícula"),
            Map.entry("recuperacion", "un resultado de recuperación"),
            Map.entry("reportes-disciplinarios", "un reporte de convivencia"),
            Map.entry("disciplina", "un reporte de convivencia"),
            Map.entry("tickets", "un ticket de mantenimiento"),
            Map.entry("derivaciones", "una derivación psicológica"),
            Map.entry("fichas-medicas", "una ficha médica"),
            Map.entry("atenciones-medicas", "una atención de tópico"),
            Map.entry("estudiantes", "datos de estudiantes"),
            Map.entry("apoderados", "datos de apoderados"),
            Map.entry("consentimientos", "un consentimiento"),
            Map.entry("documentos", "un documento"),
            Map.entry("cursos", "cursos"),
            Map.entry("docentes", "docentes"),
            Map.entry("secciones", "secciones"),
            Map.entry("horarios", "el horario"),
            Map.entry("asignaciones-academicas", "la carga académica"),
            Map.entry("aulas", "aulas"),
            Map.entry("usuarios", "cuentas de usuario"),
            Map.entry("notificaciones", "notificaciones"),
            Map.entry("institucion", "la configuración del colegio"),
            Map.entry("integridad", "la revisión de integridad"));

    private final Sinks.Many<EventoCambio> sink = Sinks.many().multicast().directBestEffort();
    private final AtomicInteger conexiones = new AtomicInteger();

    public void publicar(String modulo, String metodo, String actor, String actorUsuario) {
        String accion = switch (metodo) {
            case "POST" -> "CREAR";
            case "DELETE" -> "ELIMINAR";
            default -> "ACTUALIZAR";
        };
        String verbo = switch (accion) {
            case "CREAR" -> "registró";
            case "ELIMINAR" -> "eliminó";
            default -> "actualizó";
        };
        String objeto = NOMBRE_MODULO.getOrDefault(modulo, modulo.replace('-', ' '));
        EventoCambio evento = new EventoCambio(UUID.randomUUID().toString(), modulo, accion,
                verbo + " " + objeto, actor, actorUsuario, Instant.now());
        sink.tryEmitNext(evento);
    }

    /** Flujo de eventos visibles para alguno de los roles del usuario. */
    public Flux<EventoCambio> eventosPara(Collection<String> roles) {
        return sink.asFlux().filter(evento -> puedeVer(evento.modulo(), roles));
    }

    public Flux<Long> latidos() {
        return Flux.interval(Duration.ofSeconds(25));
    }

    public boolean puedeVer(String modulo, Collection<String> roles) {
        Set<String> audiencia = AUDIENCIA.getOrDefault(modulo, Set.of("DIRECTOR", "SUBDIRECTOR"));
        return roles.stream().anyMatch(audiencia::contains);
    }

    public int conexionesActivas() {
        return conexiones.get();
    }

    void conectado() {
        conexiones.incrementAndGet();
    }

    void desconectado() {
        conexiones.decrementAndGet();
    }
}
