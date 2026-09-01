package com.example.demo.service;

import com.example.demo.dto.DocenteDTO;
import com.example.demo.mapper.DocenteMapper;
import com.example.demo.model.Docente;
import com.example.demo.repository.DocenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocenteService {

    @Autowired
    private DocenteRepository docenteRepository;

    @Autowired
    private DocenteMapper docenteMapper;

    public List<DocenteDTO> listarTodos() {
        return docenteRepository.findAll().stream()
                .map(docenteMapper::toDto)
                .collect(Collectors.toList());
    }

    public Optional<DocenteDTO> buscarPorId(Long id) {
        return docenteRepository.findById(id).map(docenteMapper::toDto);
    }

    public DocenteDTO guardar(DocenteDTO docenteDTO) {
        Docente docente = docenteMapper.toEntity(docenteDTO);
        docente = docenteRepository.save(docente);
        return docenteMapper.toDto(docente);
    }

    public Optional<DocenteDTO> actualizar(Long id, DocenteDTO detalles) {
        return docenteRepository.findById(id).map(docente -> {
            docenteMapper.updateEntityFromDto(detalles, docente);
            return docenteMapper.toDto(docenteRepository.save(docente));
        });
    }

    public boolean eliminar(Long id) {
        if (docenteRepository.existsById(id)) {
            docenteRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public long countActivos() {
        return docenteRepository.countByEstado("ACTIVO");
    }
}