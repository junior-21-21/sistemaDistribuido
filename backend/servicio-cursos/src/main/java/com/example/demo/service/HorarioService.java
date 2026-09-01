package com.example.demo.service;

import com.example.demo.dto.HorarioDTO;
import com.example.demo.mapper.HorarioMapper;
import com.example.demo.model.Horario;
import com.example.demo.repository.HorarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class HorarioService {

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private HorarioMapper horarioMapper;

    public List<HorarioDTO> listarPorSeccion(Long seccionId) {
        return horarioRepository.findBySeccionId(seccionId).stream()
                .map(horarioMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<HorarioDTO> listarPorDocente(Long docenteId) {
        return horarioRepository.findByDocenteId(docenteId).stream()
                .map(horarioMapper::toDto)
                .collect(Collectors.toList());
    }

    public HorarioDTO guardar(HorarioDTO horarioDTO) {
        // Validar cruce de docente
        List<Horario> crucesDocente = horarioRepository.findConflictosDocente(
                horarioDTO.getDocenteId(), horarioDTO.getDiaSemana(),
                horarioDTO.getHoraInicio(), horarioDTO.getHoraFin());

        if (!crucesDocente.isEmpty()) {
            throw new IllegalArgumentException("El docente ya tiene una clase asignada en ese horario.");
        }

        // Validar cruce de aula
        List<Horario> crucesAula = horarioRepository.findConflictosAula(
                horarioDTO.getAulaId(), horarioDTO.getDiaSemana(),
                horarioDTO.getHoraInicio(), horarioDTO.getHoraFin());

        if (!crucesAula.isEmpty()) {
            throw new IllegalArgumentException("El aula física ya está ocupada en ese horario.");
        }

        Horario horario = horarioMapper.toEntity(horarioDTO);
        horario = horarioRepository.save(horario);
        return horarioMapper.toDto(horario);
    }

    public boolean eliminar(Long id) {
        if (horarioRepository.existsById(id)) {
            horarioRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
