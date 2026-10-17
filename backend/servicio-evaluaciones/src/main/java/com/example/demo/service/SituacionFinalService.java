package com.example.demo.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Nota;
import com.example.demo.repository.NotaRepository;

/**
 * Calcula la situación final sugerida de cada estudiante de una sección a partir de sus notas
 * del año (nivel final de cada competencia = último bimestre calificado) y de las reglas del MINEDU.
 * Secretaría o Dirección la revisan y confirman en «Promoción y recuperación».
 */
@Service
@Transactional(readOnly = true)
public class SituacionFinalService {

    public record Competencia(String competencia, String nivel) {
    }

    public record AreaCalculada(Long cursoId, List<Competencia> competencias, long enC) {
    }

    public record SituacionEstudiante(Long estudianteId, String situacion, String motivo,
                                      List<Long> areasConC, List<AreaCalculada> areas) {
    }

    private final NotaRepository notas;

    public SituacionFinalService(NotaRepository notas) {
        this.notas = notas;
    }

    public List<SituacionEstudiante> calcular(Long seccionId, int anio, int grado) {
        // estudiante -> curso -> competencia -> (bimestre -> nivel)
        Map<Long, Map<Long, Map<String, Map<Integer, String>>>> arbol = new LinkedHashMap<>();
        for (Nota nota : notas.findBySeccionId(seccionId)) {
            if (nota.getPeriodoAnio() != null && !String.valueOf(anio).equals(nota.getPeriodoAnio())) continue;
            String nivel = nivel(nota);
            if (nivel == null || nota.getCompetencia() == null) continue;
            arbol.computeIfAbsent(nota.getEstudianteId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(nota.getCursoId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(nota.getCompetencia().trim(), k -> new TreeMap<>())
                    .put(ReglasPromocion.numeroBimestre(nota.getBimestre()), nivel);
        }

        List<SituacionEstudiante> resultado = new ArrayList<>();
        arbol.forEach((estudianteId, cursos) -> {
            List<ReglasPromocion.Area> areas = new ArrayList<>();
            List<AreaCalculada> detalle = new ArrayList<>();
            cursos.forEach((cursoId, competencias) -> {
                List<Competencia> finales = competencias.entrySet().stream()
                        .map(e -> new Competencia(e.getKey(), ReglasPromocion.nivelFinal(e.getValue())))
                        .toList();
                List<String> niveles = finales.stream().map(Competencia::nivel).toList();
                areas.add(new ReglasPromocion.Area(cursoId, niveles));
                detalle.add(new AreaCalculada(cursoId, finales, niveles.stream().filter("C"::equals).count()));
            });
            ReglasPromocion.Resultado r = ReglasPromocion.alTerminoDelAnio(grado, areas);
            resultado.add(new SituacionEstudiante(estudianteId, r.situacion().name(), r.motivo(), r.areasConC(), detalle));
        });
        return resultado;
    }

    /** Nivel literal de la nota; si solo hay valor vigesimal se convierte a la escala del CNEB. */
    private static String nivel(Nota nota) {
        if (nota.getEscalaLiteral() != null && !nota.getEscalaLiteral().isBlank()) {
            return nota.getEscalaLiteral().trim().toUpperCase();
        }
        if (nota.getValorNumerico() == null) return null;
        double v = nota.getValorNumerico();
        if (v >= 18) return "AD";
        if (v >= 14) return "A";
        if (v >= 11) return "B";
        return "C";
    }
}
