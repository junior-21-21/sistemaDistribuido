package com.example.demo.service;

import com.example.demo.dto.AsistenciaDTO;
import com.example.demo.mapper.AsistenciaMapper;
import com.example.demo.model.Asistencia;
import com.example.demo.repository.AsistenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AsistenciaService {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private AsistenciaMapper asistenciaMapper;

    public List<AsistenciaDTO> listarPorEstudiante(Long estudianteId) {
        return asistenciaRepository.findByEstudianteId(estudianteId).stream()
                .map(asistenciaMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<AsistenciaDTO> listarPorHorario(Long horarioId) {
        return asistenciaRepository.findByHorarioId(horarioId).stream()
                .map(asistenciaMapper::toDto)
                .collect(Collectors.toList());
    }

    public AsistenciaDTO guardar(AsistenciaDTO asistenciaDTO) {
        Asistencia asistencia = asistenciaMapper.toEntity(asistenciaDTO);
        asistencia = asistenciaRepository.save(asistencia);
        return asistenciaMapper.toDto(asistencia);
    }
}
