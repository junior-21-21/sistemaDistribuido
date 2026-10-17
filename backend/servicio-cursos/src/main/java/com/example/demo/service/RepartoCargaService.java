package com.example.demo.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.AsignacionAcademica;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Horario;
import com.example.demo.model.Seccion;
import com.example.demo.repository.AsignacionAcademicaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.HorarioRepository;
import com.example.demo.repository.SeccionRepository;

/**
 * Reparte un curso (por ejemplo, Matemática en todas las secciones de un grado) entre varios
 * docentes, equilibrando su carga semanal para que nadie quede sobrecargado.
 * Primero se puede simular para ver la propuesta y luego aplicarla en una sola transacción.
 */
@Service
public class RepartoCargaService {

    public record Item(Long seccionId, Long cursoId, Integer horasSemanales) {
    }

    public record Solicitud(Integer anioEscolar, List<Long> docenteIds, List<Item> items, boolean simular) {
    }

    public record Linea(Long seccionId, String seccion, Long cursoId, String curso, int horas,
                        Long docenteId, String docente) {
    }

    public record Carga(Long docenteId, String docente, long antes, long despues, int contratadas) {
    }

    public record Resultado(boolean aplicado, List<Linea> plan, List<Carga> cargas, List<String> sinAsignar,
                            int horariosActualizados, int horariosLiberados) {
    }

    private final AsignacionAcademicaRepository asignaciones;
    private final CursoRepository cursos;
    private final DocenteRepository docentes;
    private final SeccionRepository secciones;
    private final HorarioRepository horarios;

    public RepartoCargaService(AsignacionAcademicaRepository asignaciones, CursoRepository cursos,
                               DocenteRepository docentes, SeccionRepository secciones, HorarioRepository horarios) {
        this.asignaciones = asignaciones;
        this.cursos = cursos;
        this.docentes = docentes;
        this.secciones = secciones;
        this.horarios = horarios;
    }

    @Transactional
    public Resultado repartir(Solicitud solicitud) {
        if (solicitud.anioEscolar() == null || solicitud.items() == null || solicitud.items().isEmpty()) {
            throw new IllegalArgumentException("Indica el año y al menos una sección con su curso");
        }
        if (solicitud.docenteIds() == null || solicitud.docenteIds().isEmpty()) {
            throw new IllegalArgumentException("Elige al menos un docente");
        }
        int anio = solicitud.anioEscolar();

        Map<Long, Docente> equipo = new LinkedHashMap<>();
        for (Long id : solicitud.docenteIds()) {
            Docente d = docentes.findById(id).orElseThrow(() -> new IllegalArgumentException("Docente " + id + " no existe"));
            if (!"ACTIVO".equalsIgnoreCase(d.getEstado())) {
                throw new IllegalArgumentException(d.getNombreCompleto() + " no está activo");
            }
            equipo.put(id, d);
        }

        // Asignaciones actuales de esos curso-sección: se reemplazan y liberan sus horas.
        List<AsignacionAcademica> reemplazadas = new ArrayList<>();
        List<Object[]> pendientes = new ArrayList<>();
        for (Item item : solicitud.items()) {
            Seccion seccion = secciones.findById(item.seccionId())
                    .orElseThrow(() -> new IllegalArgumentException("Sección " + item.seccionId() + " no existe"));
            Curso curso = cursos.findById(item.cursoId())
                    .orElseThrow(() -> new IllegalArgumentException("Curso " + item.cursoId() + " no existe"));
            if (!curso.getGradoId().equals(seccion.getGrado().getId())) {
                throw new IllegalArgumentException(curso.getNombre() + " no pertenece al grado de " + nombre(seccion));
            }
            List<AsignacionAcademica> actuales = asignaciones
                    .findBySeccionIdAndAnioEscolarAndActivaTrueOrderByCursoNombre(seccion.getId(), anio).stream()
                    .filter(a -> a.getCurso().getId().equals(curso.getId()))
                    .toList();
            reemplazadas.addAll(actuales);
            int horas = item.horasSemanales() != null && item.horasSemanales() > 0 ? item.horasSemanales()
                    : actuales.stream().mapToInt(AsignacionAcademica::getHorasSemanales).sum();
            if (horas <= 0) throw new IllegalArgumentException("Indica las horas de " + curso.getNombre() + " en " + nombre(seccion));
            pendientes.add(new Object[] { seccion, curso, horas });
        }

        Map<Long, Long> cargaInicial = new HashMap<>();
        Map<Long, Long> carga = new HashMap<>();
        for (Docente d : equipo.values()) {
            long actual = asignaciones.sumarHorasDocente(d.getId(), anio, null);
            long liberada = reemplazadas.stream().filter(a -> a.getDocente().getId().equals(d.getId()))
                    .mapToLong(AsignacionAcademica::getHorasSemanales).sum();
            cargaInicial.put(d.getId(), actual);
            carga.put(d.getId(), actual - liberada);
        }

        // Las cargas más grandes primero; cada una va al docente con más espacio relativo libre.
        pendientes.sort(Comparator.comparingInt((Object[] p) -> (Integer) p[2]).reversed());
        List<Linea> plan = new ArrayList<>();
        List<String> sinAsignar = new ArrayList<>();
        for (Object[] p : pendientes) {
            Seccion seccion = (Seccion) p[0];
            Curso curso = (Curso) p[1];
            int horas = (Integer) p[2];
            Docente elegido = equipo.values().stream()
                    .filter(d -> carga.get(d.getId()) + horas <= contratadas(d))
                    .min(Comparator.comparingDouble((Docente d) -> (double) carga.get(d.getId()) / contratadas(d))
                            .thenComparing(Docente::getId))
                    .orElse(null);
            if (elegido == null) {
                sinAsignar.add(curso.getNombre() + " en " + nombre(seccion) + " (" + horas + " h)");
                continue;
            }
            carga.merge(elegido.getId(), (long) horas, Long::sum);
            plan.add(new Linea(seccion.getId(), nombre(seccion), curso.getId(), curso.getNombre(), horas,
                    elegido.getId(), elegido.getNombreCompleto()));
        }

        List<Carga> cargas = equipo.values().stream()
                .map(d -> new Carga(d.getId(), d.getNombreCompleto(), cargaInicial.get(d.getId()), carga.get(d.getId()), contratadas(d)))
                .toList();

        if (solicitud.simular() || !sinAsignar.isEmpty()) {
            return new Resultado(false, plan, cargas, sinAsignar, 0, 0);
        }

        // Aplicar: reemplazar asignaciones y trasladar los bloques de horario al nuevo docente.
        Map<String, Long> anterior = new HashMap<>();
        reemplazadas.forEach(a -> anterior.put(a.getSeccion().getId() + "|" + a.getCurso().getId(), a.getDocente().getId()));
        asignaciones.deleteAll(reemplazadas);
        asignaciones.flush();
        int actualizados = 0;
        int liberados = 0;
        for (Linea linea : plan) {
            AsignacionAcademica nueva = new AsignacionAcademica();
            nueva.setCurso(cursos.getReferenceById(linea.cursoId()));
            nueva.setDocente(equipo.get(linea.docenteId()));
            nueva.setSeccion(secciones.getReferenceById(linea.seccionId()));
            nueva.setHorasSemanales(linea.horas());
            nueva.setAnioEscolar(anio);
            nueva.setActiva(true);
            asignaciones.save(nueva);

            for (Horario h : horarios.findBySeccionId(linea.seccionId())) {
                if (!h.getCurso().getId().equals(linea.cursoId()) || h.getDocente().getId().equals(linea.docenteId())) continue;
                boolean cruce = !horarios.findConflictosDocente(linea.docenteId(), h.getDiaSemana(),
                        h.getHoraInicio(), h.getHoraFin()).isEmpty();
                if (cruce) {
                    // El nuevo docente ya dicta a esa hora: el bloque queda libre para reubicarlo.
                    horarios.delete(h);
                    liberados++;
                } else {
                    h.setDocente(equipo.get(linea.docenteId()));
                    horarios.save(h);
                    actualizados++;
                }
            }
        }
        return new Resultado(true, plan, cargas, sinAsignar, actualizados, liberados);
    }

    private static int contratadas(Docente d) {
        return d.getHorasSemanalesContratadas() == null ? 30 : d.getHorasSemanalesContratadas();
    }

    private static String nombre(Seccion s) {
        return s.getGrado().getNombre() + " " + s.getNombre();
    }
}
