package com.example.demo.service;

import com.example.demo.dto.PeriodoEscolarDTO;
import com.example.demo.model.PeriodoEscolar;
import com.example.demo.repository.PeriodoEscolarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PeriodoEscolarService {

    private final PeriodoEscolarRepository periodoRepository;

    public PeriodoEscolarService(PeriodoEscolarRepository periodoRepository) {
        this.periodoRepository = periodoRepository;
    }

    public List<PeriodoEscolarDTO> listarPorAnio(Integer anio) {
        return periodoRepository.findByAnioEscolarOrderByFechaInicio(anio)
            .stream().map(this::toDTO).toList();
    }

    public Optional<PeriodoEscolarDTO> buscarActivo(Integer anio) {
        return periodoRepository.findByAnioEscolarAndEstado(anio, "ACTIVO")
            .map(this::toDTO);
    }

    public PeriodoEscolarDTO guardar(PeriodoEscolarDTO dto) {
        PeriodoEscolar p = toEntity(dto);
        return toDTO(periodoRepository.save(p));
    }

    /**
     * Congela el período escolar en bloque (festividades, huelgas, clima).
     * El Director puede especificar el motivo y las fechas de suspensión.
     */
    public Optional<PeriodoEscolarDTO> suspender(Long id, String motivo, LocalDate fechaReanudacion) {
        return periodoRepository.findById(id).map(p -> {
            p.setEstado("SUSPENDIDO");
            p.setMotivoSuspension(motivo);
            p.setFechaSuspension(LocalDate.now());
            p.setFechaReanudacion(fechaReanudacion);
            return toDTO(periodoRepository.save(p));
        });
    }

    public Optional<PeriodoEscolarDTO> reanudar(Long id) {
        return periodoRepository.findById(id).map(p -> {
            p.setEstado("ACTIVO");
            p.setFechaSuspension(null);
            return toDTO(periodoRepository.save(p));
        });
    }

    private PeriodoEscolarDTO toDTO(PeriodoEscolar p) {
        PeriodoEscolarDTO dto = new PeriodoEscolarDTO();
        dto.setId(p.getId());
        dto.setAnioEscolar(p.getAnioEscolar());
        dto.setNombre(p.getNombre());
        dto.setFechaInicio(p.getFechaInicio());
        dto.setFechaFin(p.getFechaFin());
        dto.setEstado(p.getEstado());
        dto.setMotivoSuspension(p.getMotivoSuspension());
        dto.setFechaSuspension(p.getFechaSuspension());
        dto.setFechaReanudacion(p.getFechaReanudacion());
        dto.setCreadoPor(p.getCreadoPor());
        return dto;
    }

    private PeriodoEscolar toEntity(PeriodoEscolarDTO dto) {
        PeriodoEscolar p = new PeriodoEscolar();
        p.setAnioEscolar(dto.getAnioEscolar());
        p.setNombre(dto.getNombre());
        p.setFechaInicio(dto.getFechaInicio());
        p.setFechaFin(dto.getFechaFin());
        p.setEstado(dto.getEstado() != null ? dto.getEstado() : "PLANIFICADO");
        p.setCreadoPor(dto.getCreadoPor());
        return p;
    }
}
