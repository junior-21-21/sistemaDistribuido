package com.example.demo.service;

import com.example.demo.dto.EstudianteDTO;
import com.example.demo.mapper.EstudianteMapper;
import com.example.demo.model.Estudiante;
import com.example.demo.repository.EstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class EstudianteService {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private EstudianteMapper estudianteMapper;

    public List<EstudianteDTO> listarTodos() {
        return estudianteRepository.findAll().stream()
                .map(estudianteMapper::toDto)
                .collect(Collectors.toList());
    }

    public Optional<EstudianteDTO> buscarPorId(Long id) {
        return estudianteRepository.findById(id).map(estudianteMapper::toDto);
    }

    public EstudianteDTO guardar(EstudianteDTO estudianteDTO) {
        Estudiante estudiante = estudianteMapper.toEntity(estudianteDTO);
        estudiante = estudianteRepository.save(estudiante);
        return estudianteMapper.toDto(estudiante);
    }

    public Optional<EstudianteDTO> actualizar(Long id, EstudianteDTO detalles) {
        return estudianteRepository.findById(id).map(estudiante -> {
            estudianteMapper.updateEntityFromDto(detalles, estudiante);
            return estudianteMapper.toDto(estudianteRepository.save(estudiante));
        });
    }

    public boolean eliminar(Long id) {
        return estudianteRepository.findById(id).map(estudiante -> {
            estudianteRepository.delete(estudiante);
            return true;
        }).orElse(false);
    }

    public long countTotal() {
        return estudianteRepository.count();
    }

    public long countPendientes() {
        return estudianteRepository.countByEstado("PENDIENTE");
    }
}