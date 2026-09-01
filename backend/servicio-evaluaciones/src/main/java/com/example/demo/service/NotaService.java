package com.example.demo.service;

import com.example.demo.dto.NotaDTO;
import com.example.demo.mapper.NotaMapper;
import com.example.demo.model.Nota;
import com.example.demo.repository.NotaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotaService {

    @Autowired
    private NotaRepository notaRepository;

    @Autowired
    private NotaMapper notaMapper;

    public List<NotaDTO> listarPorEstudiante(Long estudianteId) {
        return notaRepository.findByEstudianteId(estudianteId).stream()
                .map(notaMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<NotaDTO> listarPorCurso(Long cursoId) {
        return notaRepository.findByCursoId(cursoId).stream()
                .map(notaMapper::toDto)
                .collect(Collectors.toList());
    }

    public NotaDTO guardar(NotaDTO notaDTO) {
        Nota nota = notaMapper.toEntity(notaDTO);
        nota = notaRepository.save(nota);
        return notaMapper.toDto(nota);
    }
}
