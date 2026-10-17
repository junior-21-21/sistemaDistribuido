package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.PonderacionesDTO;
import com.example.demo.model.Nota;
import com.example.demo.model.PonderacionCompetencia;
import com.example.demo.repository.NotaRepository;
import com.example.demo.repository.PonderacionCompetenciaRepository;

/**
 * Pesos (%) de las columnas de evaluación. La suma no puede pasar de 100 %; el promedio
 * ponderado del bimestre se calcula con ellos en la planilla de notas.
 */
@Service
@Transactional
public class PonderacionService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final PonderacionCompetenciaRepository ponderaciones;
    private final NotaRepository notas;
    private final BimestreService bimestres;

    public PonderacionService(PonderacionCompetenciaRepository ponderaciones, NotaRepository notas,
                              BimestreService bimestres) {
        this.ponderaciones = ponderaciones;
        this.notas = notas;
        this.bimestres = bimestres;
    }

    @Transactional(readOnly = true)
    public PonderacionesDTO listar(Long seccionId, Long cursoId, String bimestre, String periodoAnio) {
        String anio = anio(periodoAnio);
        PonderacionesDTO dto = new PonderacionesDTO();
        dto.setSeccionId(seccionId);
        dto.setCursoId(cursoId);
        dto.setBimestre(bimestre);
        dto.setPeriodoAnio(anio);
        dto.setColumnas(buscar(seccionId, cursoId, bimestre, anio).stream().map(item -> {
            PonderacionesDTO.Columna columna = new PonderacionesDTO.Columna();
            columna.setCompetencia(item.getCompetencia());
            columna.setPeso(item.getPeso());
            return columna;
        }).toList());
        return dto;
    }

    /** Reemplaza todos los pesos del contexto (sección, área, bimestre y año) en una sola transacción. */
    public PonderacionesDTO guardar(PonderacionesDTO dto, String usuario) {
        String anio = anio(dto.getPeriodoAnio());
        bimestres.validarAbierto(Integer.parseInt(anio), dto.getBimestre());

        Set<String> nombres = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (PonderacionesDTO.Columna columna : dto.getColumnas()) {
            if (!nombres.add(clave(columna.getCompetencia()))) {
                throw new IllegalArgumentException("La columna «" + columna.getCompetencia().trim() + "» está repetida");
            }
            total = total.add(columna.getPeso());
        }
        if (total.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException("La suma de los porcentajes es " + total.stripTrailingZeros().toPlainString()
                    + " %; no puede pasar de 100 %");
        }

        ponderaciones.deleteAll(buscar(dto.getSeccionId(), dto.getCursoId(), dto.getBimestre(), anio));
        ponderaciones.flush();
        int orden = 0;
        for (PonderacionesDTO.Columna columna : dto.getColumnas()) {
            PonderacionCompetencia item = new PonderacionCompetencia();
            item.setSeccionId(dto.getSeccionId());
            item.setCursoId(dto.getCursoId());
            item.setBimestre(dto.getBimestre().trim().toUpperCase(Locale.ROOT));
            item.setPeriodoAnio(anio);
            item.setCompetencia(columna.getCompetencia().trim());
            item.setPeso(columna.getPeso());
            item.setOrden(orden++);
            item.setActualizadoPor(usuario == null ? "sistema" : usuario);
            item.setActualizadoEn(LocalDateTime.now());
            ponderaciones.save(item);
        }
        return listar(dto.getSeccionId(), dto.getCursoId(), dto.getBimestre(), anio);
    }

    /** Quita una columna completa: sus notas y su peso. Devuelve cuántas notas se eliminaron. */
    public int eliminarColumna(Long seccionId, Long cursoId, String bimestre, String periodoAnio, String competencia) {
        String anio = anio(periodoAnio);
        bimestres.validarAbierto(Integer.parseInt(anio), bimestre);
        String objetivo = clave(competencia);
        List<Nota> aBorrar = notas.findBySeccionIdAndCursoIdAndBimestreIgnoreCase(seccionId, cursoId, bimestre).stream()
                .filter(nota -> anio.equals(nota.getPeriodoAnio()) && objetivo.equals(clave(nota.getCompetencia())))
                .toList();
        notas.deleteAll(aBorrar);
        ponderaciones.deleteAll(buscar(seccionId, cursoId, bimestre, anio).stream()
                .filter(item -> objetivo.equals(clave(item.getCompetencia())))
                .toList());
        return aBorrar.size();
    }

    private List<PonderacionCompetencia> buscar(Long seccionId, Long cursoId, String bimestre, String anio) {
        return ponderaciones.findBySeccionIdAndCursoIdAndBimestreIgnoreCaseAndPeriodoAnioOrderByOrdenAscIdAsc(
                seccionId, cursoId, bimestre.trim(), anio);
    }

    public static String anio(String periodoAnio) {
        if (periodoAnio == null || periodoAnio.isBlank()) return String.valueOf(Year.now().getValue());
        try {
            return String.valueOf(Integer.parseInt(periodoAnio.trim()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El año académico no es válido");
        }
    }

    private static String clave(String competencia) {
        return competencia == null ? "" : competencia.trim().toLowerCase(Locale.ROOT);
    }
}
