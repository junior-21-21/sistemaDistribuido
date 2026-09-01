package com.example.demo.service;

import com.example.demo.dto.GradoDTO;
import com.example.demo.model.Grado;
import com.example.demo.model.Nivel;
import com.example.demo.repository.GradoRepository;
import com.example.demo.repository.NivelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GradoService {

    private final GradoRepository gradoRepository;
    private final NivelRepository nivelRepository;

    public GradoService(GradoRepository gradoRepository, NivelRepository nivelRepository) {
        this.gradoRepository = gradoRepository;
        this.nivelRepository = nivelRepository;
    }

    public List<GradoDTO> listarActivos() {
        return gradoRepository.findByActivoTrue().stream()
            .map(this::toDTO).toList();
    }

    public List<GradoDTO> listarPorNivel(Long nivelId) {
        return gradoRepository.findByNivelIdAndActivoTrue(nivelId).stream()
            .map(this::toDTO).toList();
    }

    public Optional<GradoDTO> buscarPorId(Long id) {
        return gradoRepository.findById(id).map(this::toDTO);
    }

    public GradoDTO guardar(GradoDTO dto) {
        Nivel nivel = nivelRepository.findById(dto.getNivelId())
            .orElseThrow(() -> new IllegalArgumentException("Nivel no encontrado: " + dto.getNivelId()));
        Grado grado = new Grado();
        grado.setNombre(dto.getNombre());
        grado.setNivel(nivel);
        grado.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        return toDTO(gradoRepository.save(grado));
    }

    public Optional<GradoDTO> actualizar(Long id, GradoDTO dto) {
        return gradoRepository.findById(id).map(g -> {
            g.setNombre(dto.getNombre());
            if (dto.getNivelId() != null) {
                Nivel nivel = nivelRepository.findById(dto.getNivelId())
                    .orElseThrow(() -> new IllegalArgumentException("Nivel no encontrado: " + dto.getNivelId()));
                g.setNivel(nivel);
            }
            if (dto.getActivo() != null) g.setActivo(dto.getActivo());
            return toDTO(gradoRepository.save(g));
        });
    }

    public boolean eliminar(Long id) {
        if (gradoRepository.existsById(id)) {
            gradoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private GradoDTO toDTO(Grado g) {
        GradoDTO dto = new GradoDTO();
        dto.setId(g.getId());
        dto.setNombre(g.getNombre());
        dto.setNivelId(g.getNivel().getId());
        dto.setNivelNombre(g.getNivel().getNombre());
        dto.setActivo(g.getActivo());
        return dto;
    }
}
