package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Estudiante;
import com.example.demo.model.NominaMatricula;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.repository.NominaMatriculaRepository;

/**
 * Nómina de matrícula por sección. Secretaría la revisa e imprime; Dirección la aprueba y firma
 * (RM 432-2020-MINEDU). Si después cambia el número de estudiantes, la nómina queda desactualizada
 * y Dirección debe volver a aprobarla.
 */
@Service
@Transactional(readOnly = true)
public class NominaService {

    public record EstadoNomina(Long seccionId, Integer anioEscolar, long estudiantes, boolean aprobada,
                               boolean desactualizada, Integer totalAprobado, String aprobadaPor,
                               LocalDateTime fechaAprobacion, String observacion) {
    }

    public record FilaNomina(int numero, Long estudianteId, String dni, String apellidos, String nombres) {
    }

    private final NominaMatriculaRepository nominas;
    private final EstudianteRepository estudiantes;

    public NominaService(NominaMatriculaRepository nominas, EstudianteRepository estudiantes) {
        this.nominas = nominas;
        this.estudiantes = estudiantes;
    }

    public List<EstadoNomina> estado(Integer anio) {
        int anioEscolar = anio == null ? Year.now().getValue() : anio;
        Map<Long, Long> conteo = new HashMap<>();
        for (Object[] fila : estudiantes.contarActivosPorSeccion()) {
            conteo.put((Long) fila[0], (Long) fila[1]);
        }
        Map<Long, NominaMatricula> aprobadas = nominas.findByAnioEscolar(anioEscolar).stream()
                .collect(Collectors.toMap(NominaMatricula::getSeccionId, Function.identity()));
        java.util.Set<Long> secciones = new java.util.TreeSet<>(conteo.keySet());
        secciones.addAll(aprobadas.keySet());
        return secciones.stream().map(id -> aEstado(id, anioEscolar, conteo.getOrDefault(id, 0L), aprobadas.get(id)))
                .toList();
    }

    public List<FilaNomina> detalle(Long seccionId) {
        List<Estudiante> lista = estudiantes.findBySeccionIdAndEstadoOrderByApellidoAscNombreAsc(seccionId, "ACTIVO");
        java.util.concurrent.atomic.AtomicInteger n = new java.util.concurrent.atomic.AtomicInteger();
        return lista.stream().map(e -> new FilaNomina(n.incrementAndGet(), e.getId(), e.getDni(), e.getApellido(),
                e.getNombre())).toList();
    }

    @Transactional
    public EstadoNomina aprobar(Long seccionId, Integer anio, String observacion, String director) {
        int anioEscolar = anio == null ? Year.now().getValue() : anio;
        long total = estudiantes.findBySeccionIdAndEstadoOrderByApellidoAscNombreAsc(seccionId, "ACTIVO").size();
        if (total == 0) {
            throw new IllegalArgumentException("La sección no tiene estudiantes matriculados");
        }
        NominaMatricula nomina = nominas.findBySeccionIdAndAnioEscolar(seccionId, anioEscolar)
                .orElseGet(NominaMatricula::new);
        nomina.setSeccionId(seccionId);
        nomina.setAnioEscolar(anioEscolar);
        nomina.setTotalEstudiantes((int) total);
        nomina.setAprobadaPor(director);
        nomina.setFechaAprobacion(LocalDateTime.now());
        nomina.setObservacion(observacion == null || observacion.isBlank() ? null : observacion.trim());
        return aEstado(seccionId, anioEscolar, total, nominas.save(nomina));
    }

    private EstadoNomina aEstado(Long seccionId, int anio, long total, NominaMatricula nomina) {
        if (nomina == null) {
            return new EstadoNomina(seccionId, anio, total, false, false, null, null, null, null);
        }
        boolean desactualizada = nomina.getTotalEstudiantes() != total;
        return new EstadoNomina(seccionId, anio, total, true, desactualizada, nomina.getTotalEstudiantes(),
                nomina.getAprobadaPor(), nomina.getFechaAprobacion(), nomina.getObservacion());
    }
}
