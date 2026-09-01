package com.example.demo.service;

import com.example.demo.dto.ReporteDisciplinarioDTO;
import com.example.demo.mapper.ReporteDisciplinarioMapper;
import com.example.demo.model.ReporteDisciplinario;
import com.example.demo.repository.ReporteDisciplinarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReporteDisciplinarioService {

    @Autowired
    private ReporteDisciplinarioRepository repository;

    @Autowired
    private ReporteDisciplinarioMapper mapper;

    public List<ReporteDisciplinarioDTO> listarPorEstudiante(Long estudianteId) {
        return repository.findByEstudianteId(estudianteId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public ReporteDisciplinarioDTO guardar(ReporteDisciplinarioDTO dto) {
        ReporteDisciplinario entidad = mapper.toEntity(dto);
        entidad = repository.save(entidad);
        return mapper.toDto(entidad);
    }
}
