package com.example.demo.service;

import com.example.demo.dto.NotaDTO;
import com.example.demo.mapper.NotaMapper;
import com.example.demo.model.Nota;
import com.example.demo.repository.NotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.example.demo.service.AsignacionAcademicaClient.AsignacionScope;

@Service
@Transactional
public class NotaService {

    @Autowired
    private NotaRepository notaRepository;

    @Autowired
    private NotaMapper notaMapper;

    @Autowired
    private BimestreService bimestreService;

    public List<NotaDTO> listarPorEstudiante(Long estudianteId, List<AsignacionScope> alcance) {
        return filtrar(notaRepository.findByEstudianteId(estudianteId), alcance).stream()
                .map(notaMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<NotaDTO> listarPorCurso(Long cursoId, List<AsignacionScope> alcance) {
        return filtrar(notaRepository.findByCursoId(cursoId), alcance).stream()
                .map(notaMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<NotaDTO> listarPorEstudiante(Long estudianteId) {
        return listarPorEstudiante(estudianteId, null);
    }

    public NotaDTO buscarPorId(Long id) {
        return notaRepository.findById(id).map(notaMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Nota no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<NotaDTO> listar(Long seccionId, String bimestre, List<AsignacionScope> alcance) {
        List<Nota> notas;
        if (seccionId != null && bimestre != null && !bimestre.isBlank()) {
            notas = notaRepository.findBySeccionIdAndBimestre(seccionId, bimestre);
        } else if (seccionId != null) {
            notas = notaRepository.findBySeccionId(seccionId);
        } else {
            notas = notaRepository.findAll();
        }
        return filtrar(notas, alcance).stream().map(notaMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<NotaDTO> listarPaginado(Long estudianteId, Long seccionId, Long cursoId,
                                        String bimestre, String competencia, Pageable pageable,
                                        List<AsignacionScope> alcance) {
        Specification<Nota> filtros = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (estudianteId != null) predicates.add(cb.equal(root.get("estudianteId"), estudianteId));
            if (seccionId != null) predicates.add(cb.equal(root.get("seccionId"), seccionId));
            if (cursoId != null) predicates.add(cb.equal(root.get("cursoId"), cursoId));
            if (bimestre != null && !bimestre.isBlank()) predicates.add(cb.equal(root.get("bimestre"), bimestre));
            if (competencia != null && !competencia.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("competencia")), "%" + competencia.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (alcance != null) {
                if (alcance.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    Predicate[] asignadas = alcance.stream().map(item -> cb.and(
                            cb.equal(root.get("cursoId"), item.cursoId()),
                            cb.equal(root.get("docenteId"), item.docenteId()),
                            cb.equal(root.get("seccionId"), item.seccionId()),
                            cb.equal(root.get("periodoAnio"), String.valueOf(item.anioEscolar()))))
                            .toArray(Predicate[]::new);
                    predicates.add(cb.or(asignadas));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return notaRepository.findAll(filtros, pageable).map(notaMapper::toDto);
    }

    private List<Nota> filtrar(List<Nota> notas, List<AsignacionScope> alcance) {
        if (alcance == null) return notas;
        return notas.stream().filter(nota -> alcance.stream().anyMatch(item ->
                item.cursoId().equals(nota.getCursoId())
                        && item.docenteId().equals(nota.getDocenteId())
                        && item.seccionId().equals(nota.getSeccionId())
                        && String.valueOf(item.anioEscolar()).equals(nota.getPeriodoAnio())))
                .toList();
    }

    public NotaDTO guardar(NotaDTO notaDTO) {
        validarBimestreAbierto(notaDTO);
        Nota nota = notaMapper.toEntity(notaDTO);
        nota.setId(null);
        nota = notaRepository.save(nota);
        return notaMapper.toDto(nota);
    }

    public List<NotaDTO> guardarTodas(List<NotaDTO> notasDTO) {
        if (notasDTO.isEmpty()) {
            throw new IllegalArgumentException("Debe enviar al menos una calificación");
        }
        NotaDTO contexto = notasDTO.get(0);
        validarBimestreAbierto(contexto);
        boolean contextoUnico = notasDTO.stream().allMatch(dto ->
                contexto.getSeccionId().equals(dto.getSeccionId())
                && contexto.getCursoId().equals(dto.getCursoId())
                && contexto.getBimestre().equalsIgnoreCase(dto.getBimestre()));
        if (!contextoUnico) {
            throw new IllegalArgumentException(
                    "Todas las calificaciones deben pertenecer al mismo curso, sección y bimestre");
        }

        Map<String, Nota> existentes = notaRepository
                .findBySeccionIdAndCursoIdAndBimestreIgnoreCase(
                        contexto.getSeccionId(), contexto.getCursoId(), contexto.getBimestre())
                .stream()
                .collect(Collectors.toMap(this::claveNatural, Function.identity(), (primera, duplicada) -> primera));

        List<Nota> notas = notasDTO.stream().map(dto -> {
            Nota nota = existentes.get(claveNatural(dto.getEstudianteId(), dto.getCompetencia()));
            if (nota == null) {
                nota = notaMapper.toEntity(dto);
                nota.setId(null);
            } else {
                notaMapper.updateEntity(dto, nota);
            }
            return nota;
        }).toList();
        return notaRepository.saveAll(notas).stream()
                .map(notaMapper::toDto)
                .collect(Collectors.toList());
    }

    public NotaDTO actualizar(Long id, NotaDTO notaDTO) {
        Nota nota = notaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nota no encontrada"));
        validarBimestreAbierto(notaMapper.toDto(nota));
        validarBimestreAbierto(notaDTO);
        notaMapper.updateEntity(notaDTO, nota);
        return notaMapper.toDto(notaRepository.save(nota));
    }

    public void eliminar(Long id) {
        Nota nota = notaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nota no encontrada"));
        validarBimestreAbierto(notaMapper.toDto(nota));
        notaRepository.deleteById(id);
    }

    private void validarBimestreAbierto(NotaDTO nota) {
        int anio;
        try {
            anio = nota.getPeriodoAnio() == null || nota.getPeriodoAnio().isBlank()
                    ? java.time.Year.now().getValue() : Integer.parseInt(nota.getPeriodoAnio());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El año académico no es válido");
        }
        bimestreService.validarAbierto(anio, nota.getBimestre());
    }

    private String claveNatural(Nota nota) {
        return claveNatural(nota.getEstudianteId(), nota.getCompetencia());
    }

    private String claveNatural(Long estudianteId, String competencia) {
        return estudianteId + "|" + competencia.trim().toLowerCase(Locale.ROOT);
    }
}
