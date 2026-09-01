package com.example.demo.service;

import com.example.demo.dto.NivelDTO;
import com.example.demo.model.Nivel;
import com.example.demo.repository.NivelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NivelService {

    private final NivelRepository nivelRepository;

    public NivelService(NivelRepository nivelRepository) {
        this.nivelRepository = nivelRepository;
    }

    public List<NivelDTO> listarActivos() {
        return nivelRepository.findByActivoTrue().stream()
            .map(this::toDTO).toList();
    }

    public List<NivelDTO> listarTodos() {
        return nivelRepository.findAll().stream()
            .map(this::toDTO).toList();
    }

    public Optional<NivelDTO> buscarPorId(Long id) {
        return nivelRepository.findById(id).map(this::toDTO);
    }

    public NivelDTO guardar(NivelDTO dto) {
        Nivel nivel = toEntity(dto);
        return toDTO(nivelRepository.save(nivel));
    }

    public Optional<NivelDTO> actualizar(Long id, NivelDTO dto) {
        return nivelRepository.findById(id).map(n -> {
            n.setNombre(dto.getNombre());
            n.setDescripcion(dto.getDescripcion());
            if (dto.getActivo() != null) n.setActivo(dto.getActivo());
            return toDTO(nivelRepository.save(n));
        });
    }

    public boolean eliminar(Long id) {
        if (nivelRepository.existsById(id)) {
            nivelRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private NivelDTO toDTO(Nivel n) {
        NivelDTO dto = new NivelDTO();
        dto.setId(n.getId());
        dto.setNombre(n.getNombre());
        dto.setDescripcion(n.getDescripcion());
        dto.setActivo(n.getActivo());
        return dto;
    }

    private Nivel toEntity(NivelDTO dto) {
        Nivel n = new Nivel();
        n.setNombre(dto.getNombre());
        n.setDescripcion(dto.getDescripcion());
        n.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        return n;
    }
}
