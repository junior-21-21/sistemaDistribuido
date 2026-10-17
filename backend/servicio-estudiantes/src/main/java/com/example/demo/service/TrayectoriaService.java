package com.example.demo.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.AreaPendiente;
import com.example.demo.model.Estudiante;
import com.example.demo.model.Matricula;
import com.example.demo.repository.AreaPendienteRepository;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.repository.MatriculaRepository;

/**
 * Trayectoria escolar según el flujo de una IE pública (MINEDU / SIAGIE):
 * matrícula anual (gratuita, sin condicionamientos), traslados, promoción de grado y
 * recuperación pedagógica de áreas pendientes.
 */
@Service
public class TrayectoriaService {

    public static final Set<String> TIPOS = Set.of("NUEVO_INGRESO", "CONTINUIDAD", "TRASLADO_ENTRANTE");
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    public record DatosMatricula(String tipo, String ieProcedencia, String codigoModularProcedencia) {
    }

    public record TrasladoSalida(String ieDestino, String codigoModular, LocalDate fecha, String motivo) {
    }

    /**
     * areasPendientes: áreas que el promovido lleva por subsanar durante el año siguiente.
     * areasRecuperacion: áreas con competencias en C que irá a recuperar (situación RECUPERACION).
     */
    public record Decision(Long estudianteId, String situacion, Long gradoDestinoId, Long seccionDestinoId,
                           List<String> areasPendientes, List<String> areasRecuperacion) {
    }

    /** Cierre de la recuperación: con el resultado de cada área, la norma define si pasa o permanece. */
    public record CierreRecuperacion(Integer anioOrigen, Long gradoDestinoId, Long seccionDestinoId,
                                     Long seccionPermanenciaId, Boolean ultimoGrado) {
    }

    public record ResultadoCierre(String situacion, String areaPorSubsanar, String detalle) {
    }

    public record Promocion(Integer anioOrigen, String gradoOrigen, List<Decision> decisiones) {
    }

    public record ResumenPromocion(int promovidos, int permanecen, int enRecuperacion, int areasRegistradas) {
    }

    public record AreaPendienteVista(Long id, Long estudianteId, String estudiante, Long seccionId, String cursoNombre,
                                     String gradoOrigen, Integer anioOrigen, String estado, String calificacion,
                                     String observacion, LocalDate fechaResultado, String tipo, Long gradoId,
                                     String situacionOrigen) {
    }

    public record Trayectoria(List<Matricula> matriculas, List<AreaPendienteVista> areasPendientes) {
    }

    private final MatriculaRepository matriculas;
    private final AreaPendienteRepository areas;
    private final EstudianteRepository estudiantes;

    public TrayectoriaService(MatriculaRepository matriculas, AreaPendienteRepository areas, EstudianteRepository estudiantes) {
        this.matriculas = matriculas;
        this.areas = areas;
        this.estudiantes = estudiantes;
    }

    public static int anioActual() {
        return LocalDate.now(LIMA).getYear();
    }

    /** Registra (o actualiza) la matrícula del año en curso al matricular o editar al estudiante. */
    @Transactional
    public Matricula registrarMatricula(Estudiante estudiante, DatosMatricula datos, String usuario) {
        String tipo = datos == null || datos.tipo() == null ? "NUEVO_INGRESO" : datos.tipo().trim().toUpperCase(Locale.ROOT);
        if (!TIPOS.contains(tipo)) throw new IllegalArgumentException("Tipo de matrícula no válido");
        if ("TRASLADO_ENTRANTE".equals(tipo) && (datos.ieProcedencia() == null || datos.ieProcedencia().isBlank())) {
            throw new IllegalArgumentException("Para un traslado indica la institución educativa de procedencia");
        }
        int anio = anioActual();
        Matricula m = matriculas.findByEstudianteIdAndAnioEscolar(estudiante.getId(), anio).orElseGet(Matricula::new);
        m.setEstudianteId(estudiante.getId());
        m.setAnioEscolar(anio);
        m.setTipo(tipo);
        m.setGradoId(estudiante.getGradoId());
        m.setSeccionId(estudiante.getSeccionId());
        if (datos != null) {
            m.setIeProcedencia(limpiar(datos.ieProcedencia(), 150));
            m.setCodigoModularProcedencia(limpiar(datos.codigoModularProcedencia(), 12));
        }
        m.setRegistradoPor(usuario);
        return matriculas.save(m);
    }

    @Transactional(readOnly = true)
    public Trayectoria trayectoria(Long estudianteId) {
        Estudiante e = estudiantes.findById(estudianteId).orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        return new Trayectoria(matriculas.findByEstudianteIdOrderByAnioEscolarDesc(estudianteId),
                areas.findByEstudianteIdOrderByAnioOrigenDesc(estudianteId).stream().map(a -> vista(a, e)).toList());
    }

    /** Traslado a otra IE: el estudiante deja de estar activo, pero su historial se conserva. */
    @Transactional
    public Matricula trasladarSalida(Long estudianteId, TrasladoSalida datos, String usuario) {
        if (datos == null || datos.ieDestino() == null || datos.ieDestino().isBlank()) {
            throw new IllegalArgumentException("Indica la institución educativa de destino");
        }
        Estudiante e = estudiantes.findById(estudianteId).orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        if (!"ACTIVO".equals(e.getEstado())) throw new IllegalArgumentException("El estudiante ya no está activo");
        int anio = anioActual();
        Matricula m = matriculas.findByEstudianteIdAndAnioEscolar(estudianteId, anio).orElseGet(() -> {
            Matricula nueva = new Matricula();
            nueva.setEstudianteId(estudianteId);
            nueva.setAnioEscolar(anio);
            nueva.setTipo("CONTINUIDAD");
            nueva.setGradoId(e.getGradoId());
            nueva.setSeccionId(e.getSeccionId());
            nueva.setRegistradoPor(usuario);
            return nueva;
        });
        m.setSituacionFinal("TRASLADADO");
        m.setIeDestino(limpiar(datos.ieDestino(), 150));
        m.setCodigoModularDestino(limpiar(datos.codigoModular(), 12));
        m.setFechaTraslado(datos.fecha() == null ? LocalDate.now(LIMA) : datos.fecha());
        m.setMotivoTraslado(limpiar(datos.motivo(), 300));
        e.setEstado("TRASLADADO");
        estudiantes.save(e);
        return matriculas.save(m);
    }

    /**
     * Cierre del año: por cada estudiante, PROMOVIDO (pasa al grado/sección destino, aunque
     * lleve áreas por recuperar), PERMANECE (repite el grado) o RECUPERACION (decide tras el
     * periodo de recuperación). Crea la matrícula de continuidad del año siguiente.
     */
    @Transactional
    public ResumenPromocion promover(Promocion promocion, String usuario) {
        if (promocion == null || promocion.decisiones() == null || promocion.decisiones().isEmpty()) {
            throw new IllegalArgumentException("No hay estudiantes en la promoción");
        }
        int anioOrigen = promocion.anioOrigen() == null ? anioActual() : promocion.anioOrigen();
        String gradoOrigen = promocion.gradoOrigen() == null || promocion.gradoOrigen().isBlank()
                ? "Grado anterior" : promocion.gradoOrigen().trim();
        int promovidos = 0, permanecen = 0, recuperacion = 0, areasNuevas = 0;

        for (Decision d : promocion.decisiones()) {
            Estudiante e = estudiantes.findById(d.estudianteId())
                    .orElseThrow(() -> new IllegalArgumentException("Estudiante " + d.estudianteId() + " no encontrado"));
            String situacion = d.situacion() == null ? "" : d.situacion().trim().toUpperCase(Locale.ROOT);
            if (!Set.of("PROMOVIDO", "PERMANECE", "RECUPERACION", "EGRESADO").contains(situacion)) {
                throw new IllegalArgumentException("Situación no válida para " + e.getNombreCompleto());
            }
            Matricula actual = matriculas.findByEstudianteIdAndAnioEscolar(e.getId(), anioOrigen).orElseGet(() -> {
                Matricula nueva = new Matricula();
                nueva.setEstudianteId(e.getId());
                nueva.setAnioEscolar(anioOrigen);
                nueva.setTipo("CONTINUIDAD");
                nueva.setGradoId(e.getGradoId());
                nueva.setSeccionId(e.getSeccionId());
                nueva.setRegistradoPor(usuario);
                return nueva;
            });
            actual.setSituacionFinal(situacion);
            matriculas.save(actual);

            if ("RECUPERACION".equals(situacion)) {
                List<String> enRecuperacion = d.areasRecuperacion() == null ? List.of() : d.areasRecuperacion();
                if (enRecuperacion.stream().allMatch(a -> a == null || a.isBlank())) {
                    throw new IllegalArgumentException("Indica las áreas que " + e.getNombreCompleto() + " debe recuperar");
                }
                for (String curso : enRecuperacion) {
                    if (registrarArea(e.getId(), curso, gradoOrigen, anioOrigen, "RECUPERACION", usuario)) areasNuevas++;
                }
                recuperacion++;
                continue;
            }
            for (String curso : d.areasPendientes() == null ? List.<String>of() : d.areasPendientes()) {
                if (registrarArea(e.getId(), curso, gradoOrigen, anioOrigen, "SUBSANACION", usuario)) areasNuevas++;
            }
            aplicarPase(e, situacion, d.gradoDestinoId(), d.seccionDestinoId(), anioOrigen, usuario);
            if ("PERMANECE".equals(situacion)) permanecen++; else promovidos++;
        }
        return new ResumenPromocion(promovidos, permanecen, recuperacion, areasNuevas);
    }

    /**
     * Mueve al estudiante según su situación final y crea la matrícula de continuidad del año siguiente.
     * EGRESADO: culminó 5.° de Secundaria; deja de estar activo y no se crea matrícula siguiente.
     */
    private void aplicarPase(Estudiante e, String situacion, Long gradoDestinoId, Long seccionDestinoId,
                             int anioOrigen, String usuario) {
        if ("EGRESADO".equals(situacion)) {
            e.setEstado("EGRESADO");
            estudiantes.save(e);
            return;
        }
        if ("PROMOVIDO".equals(situacion)) {
            if (gradoDestinoId == null || seccionDestinoId == null) {
                throw new IllegalArgumentException("Elige la sección de destino para " + e.getNombreCompleto());
            }
            e.setGradoId(gradoDestinoId);
            e.setSeccionId(seccionDestinoId);
        } else if (seccionDestinoId != null) {
            e.setSeccionId(seccionDestinoId);
        }
        estudiantes.save(e);
        int siguiente = anioOrigen + 1;
        Matricula continuidad = matriculas.findByEstudianteIdAndAnioEscolar(e.getId(), siguiente).orElseGet(Matricula::new);
        continuidad.setEstudianteId(e.getId());
        continuidad.setAnioEscolar(siguiente);
        continuidad.setTipo("CONTINUIDAD");
        continuidad.setGradoId(e.getGradoId());
        continuidad.setSeccionId(e.getSeccionId());
        continuidad.setRegistradoPor(usuario);
        matriculas.save(continuidad);
    }

    private boolean registrarArea(Long estudianteId, String curso, String gradoOrigen, int anioOrigen, String tipo, String usuario) {
        if (curso == null || curso.isBlank()) return false;
        String nombre = limpiar(curso, 150);
        if (areas.findByEstudianteIdAndCursoNombreAndAnioOrigenAndTipo(estudianteId, nombre, anioOrigen, tipo).isPresent()) {
            return false;
        }
        AreaPendiente area = new AreaPendiente();
        area.setEstudianteId(estudianteId);
        area.setCursoNombre(nombre);
        area.setGradoOrigen(limpiar(gradoOrigen, 60));
        area.setAnioOrigen(anioOrigen);
        area.setTipo(tipo);
        area.setRegistradoPor(usuario);
        areas.save(area);
        return true;
    }

    /**
     * Cierra la recuperación pedagógica de un estudiante (RVM 094-2020, mod. RVM 048-2024): si recuperó
     * todas las áreas, es promovido; si queda un área en C, es promovido y la lleva por subsanar hasta
     * diciembre del año siguiente; si quedan dos o más, permanece en el grado.
     */
    @Transactional
    public ResultadoCierre cerrarRecuperacion(Long estudianteId, CierreRecuperacion datos, String usuario) {
        Estudiante e = estudiantes.findById(estudianteId).orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        int anioOrigen = datos == null || datos.anioOrigen() == null ? anioActual() - 1 : datos.anioOrigen();
        List<AreaPendiente> enRecuperacion = areas.findByEstudianteIdAndAnioOrigenAndTipo(estudianteId, anioOrigen, "RECUPERACION");
        if (enRecuperacion.isEmpty()) {
            throw new IllegalArgumentException(e.getNombreCompleto() + " no tiene áreas en recuperación del año " + anioOrigen);
        }
        if (enRecuperacion.stream().anyMatch(a -> "PENDIENTE".equals(a.getEstado()))) {
            throw new IllegalArgumentException("Registra primero el resultado de todas las áreas en recuperación");
        }
        List<AreaPendiente> noRecuperadas = enRecuperacion.stream().filter(a -> "NO_SUBSANADA".equals(a.getEstado())).toList();
        boolean ultimoGrado = datos != null && Boolean.TRUE.equals(datos.ultimoGrado());
        String situacion = noRecuperadas.size() >= 2 ? "PERMANECE" : (ultimoGrado ? "EGRESADO" : "PROMOVIDO");

        Matricula actual = matriculas.findByEstudianteIdAndAnioEscolar(estudianteId, anioOrigen)
                .orElseThrow(() -> new IllegalArgumentException("No hay matrícula del año " + anioOrigen));
        if (!"RECUPERACION".equals(actual.getSituacionFinal())) {
            throw new IllegalArgumentException("La recuperación de " + e.getNombreCompleto() + " ya fue cerrada");
        }
        actual.setSituacionFinal(situacion);
        matriculas.save(actual);

        String areaPorSubsanar = null;
        if (noRecuperadas.size() == 1) {
            AreaPendiente pendiente = noRecuperadas.get(0);
            areaPorSubsanar = pendiente.getCursoNombre();
            registrarArea(estudianteId, areaPorSubsanar, pendiente.getGradoOrigen(), anioOrigen, "SUBSANACION", usuario);
        }
        aplicarPase(e, situacion, datos == null ? null : datos.gradoDestinoId(),
                "PERMANECE".equals(situacion) ? (datos == null ? null : datos.seccionPermanenciaId())
                        : (datos == null ? null : datos.seccionDestinoId()), anioOrigen, usuario);

        String detalle = switch (noRecuperadas.size()) {
            case 0 -> "Recuperó todas las áreas";
            case 1 -> "Pasa de grado y subsana " + areaPorSubsanar + " hasta diciembre";
            default -> "No recuperó " + noRecuperadas.size() + " áreas: permanece en el grado";
        };
        return new ResultadoCierre(situacion, areaPorSubsanar, detalle);
    }

    @Transactional(readOnly = true)
    public List<AreaPendienteVista> listarAreas(String estado, String tipo) {
        String e = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase(Locale.ROOT);
        String t = tipo == null || tipo.isBlank() ? null : tipo.trim().toUpperCase(Locale.ROOT);
        List<AreaPendiente> lista;
        if (t != null && e != null) lista = areas.findByTipoAndEstadoOrderByAnioOrigenDescCursoNombreAsc(t, e);
        else if (t != null) lista = areas.findByTipoOrderByAnioOrigenDescCursoNombreAsc(t);
        else if (e != null) lista = areas.findByEstadoOrderByAnioOrigenDescCursoNombreAsc(e);
        else lista = areas.findAllByOrderByAnioOrigenDescCursoNombreAsc();
        Map<Long, Estudiante> porId = estudiantes.findAllById(lista.stream().map(AreaPendiente::getEstudianteId).distinct().toList())
                .stream().collect(Collectors.toMap(Estudiante::getId, Function.identity()));
        return lista.stream().filter(a -> porId.containsKey(a.getEstudianteId()))
                .map(a -> vista(a, porId.get(a.getEstudianteId()))).toList();
    }

    @Transactional
    public AreaPendienteVista registrarResultado(Long id, String estado, String calificacion, String observacion, String usuario) {
        AreaPendiente area = areas.findById(id).orElseThrow(() -> new IllegalArgumentException("Área pendiente no encontrada"));
        String nuevoEstado = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SUBSANADA", "NO_SUBSANADA", "PENDIENTE").contains(nuevoEstado)) {
            throw new IllegalArgumentException("Estado no válido");
        }
        String nota = calificacion == null || calificacion.isBlank() ? null : calificacion.trim().toUpperCase(Locale.ROOT);
        if (nota != null && !Set.of("AD", "A", "B", "C").contains(nota)) {
            throw new IllegalArgumentException("La calificación debe ser AD, A, B o C");
        }
        area.setEstado(nuevoEstado);
        area.setCalificacion(nota);
        area.setObservacion(limpiar(observacion, 300));
        area.setFechaResultado("PENDIENTE".equals(nuevoEstado) ? null : LocalDate.now(LIMA));
        area.setRegistradoPor(usuario);
        Estudiante e = estudiantes.findById(area.getEstudianteId()).orElseThrow();
        return vista(areas.save(area), e);
    }

    private AreaPendienteVista vista(AreaPendiente a, Estudiante e) {
        return new AreaPendienteVista(a.getId(), e.getId(), e.getApellido() + ", " + e.getNombre(), e.getSeccionId(),
                a.getCursoNombre(), a.getGradoOrigen(), a.getAnioOrigen(), a.getEstado(), a.getCalificacion(),
                a.getObservacion(), a.getFechaResultado(), a.getTipo(), e.getGradoId(),
                // Situación final del año de origen: RECUPERACION mientras el caso siga abierto
                matriculas.findByEstudianteIdAndAnioEscolar(e.getId(), a.getAnioOrigen())
                        .map(Matricula::getSituacionFinal).orElse(null));
    }

    private static String limpiar(String valor, int max) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.trim();
        return limpio.length() > max ? limpio.substring(0, max) : limpio;
    }
}
