package com.example.demo.service;

import com.example.demo.dto.CursoDTO;
import com.example.demo.mapper.CursoMapper;
import com.example.demo.model.Curso;
import com.example.demo.repository.CursoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CursoService {

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private CursoMapper cursoMapper;

    public List<CursoDTO> listarTodos() {
        return cursoRepository.findAll().stream()
                .map(cursoMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<CursoDTO> listarPorGrado(Long gradoId) {
        return cursoRepository.findByGradoId(gradoId).stream()
                .map(cursoMapper::toDto)
                .collect(Collectors.toList());
    }

    public Optional<CursoDTO> buscarPorId(Long id) {
        return cursoRepository.findById(id).map(cursoMapper::toDto);
    }

    public CursoDTO guardar(CursoDTO cursoDTO) {
        Curso curso = cursoMapper.toEntity(cursoDTO);
        curso = cursoRepository.save(curso);
        return cursoMapper.toDto(curso);
    }

    public Optional<CursoDTO> actualizar(Long id, CursoDTO detalles) {
        return cursoRepository.findById(id).map(curso -> {
            cursoMapper.updateEntityFromDto(detalles, curso);
            return cursoMapper.toDto(cursoRepository.save(curso));
        });
    }

    public boolean eliminar(Long id) {
        if (cursoRepository.existsById(id)) {
            cursoRepository.deleteById(id);
            return true;
        }
        return false;
    }
}