package com.example.demo.service;

import com.example.demo.dto.SeccionDTO;
import com.example.demo.model.Grado;
import com.example.demo.model.Seccion;
import com.example.demo.repository.GradoRepository;
import com.example.demo.repository.SeccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SeccionService {

    private final SeccionRepository seccionRepository;
    private final GradoRepository gradoRepository;

    public SeccionService(SeccionRepository seccionRepository, GradoRepository gradoRepository) {
        this.seccionRepository = seccionRepository;
        this.gradoRepository = gradoRepository;
    }

    public List<SeccionDTO> listarPorAnio(Integer anioEscolar) {
        return seccionRepository.findByAnioEscolarAndActivaTrue(anioEscolar).stream()
            .map(this::toDTO).toList();
    }

    public List<SeccionDTO> listarPorGrado(Long gradoId) {
        return seccionRepository.findByGradoIdAndActivaTrue(gradoId).stream()
            .map(this::toDTO).toList();
    }

    public Optional<SeccionDTO> buscarPorId(Long id) {
        return seccionRepository.findById(id).map(this::toDTO);
    }

    public SeccionDTO guardar(SeccionDTO dto) {
        Grado grado = gradoRepository.findById(dto.getGradoId())
            .orElseThrow(() -> new IllegalArgumentException("Grado no encontrado: " + dto.getGradoId()));

        // Validar duplicado
        boolean existe = seccionRepository.existsByNombreIgnoreCaseAndGradoIdAndTurnoAndAnioEscolar(
            dto.getNombre(), dto.getGradoId(), dto.getTurno(), dto.getAnioEscolar());
        if (existe) {
            throw new IllegalArgumentException(
                "Ya existe una sección '" + dto.getNombre() + "' en ese grado, turno y año escolar.");
        }

        Seccion s = new Seccion();
        s.setNombre(dto.getNombre());
        s.setGrado(grado);
        s.setTurno(dto.getTurno());
        s.setCapacidadMaxima(dto.getCapacidadMaxima() != null ? dto.getCapacidadMaxima() : 35);
        s.setActiva(dto.getActiva() != null ? dto.getActiva() : true);
        s.setAnioEscolar(dto.getAnioEscolar());
        s.setDocenteTutorId(dto.getDocenteTutorId());
        return toDTO(seccionRepository.save(s));
    }

    public Optional<SeccionDTO> actualizar(Long id, SeccionDTO dto) {
        return seccionRepository.findById(id).map(s -> {
            // Validar duplicado excluyendo el propio registro
            if (dto.getNombre() != null && dto.getGradoId() != null && dto.getTurno() != null && dto.getAnioEscolar() != null) {
                Long gradoIdCheck = dto.getGradoId() != null ? dto.getGradoId() : s.getGrado().getId();
                boolean existe = seccionRepository.existsByNombreIgnoreCaseAndGradoIdAndTurnoAndAnioEscolarAndIdNot(
                    dto.getNombre(), gradoIdCheck, dto.getTurno(), dto.getAnioEscolar(), id);
                if (existe) {
                    throw new IllegalArgumentException(
                        "Ya existe una sección '" + dto.getNombre() + "' en ese grado, turno y año escolar.");
                }
            }
            s.setNombre(dto.getNombre());
            if (dto.getGradoId() != null) {
                Grado grado = gradoRepository.findById(dto.getGradoId())
                    .orElseThrow(() -> new IllegalArgumentException("Grado no encontrado: " + dto.getGradoId()));
                s.setGrado(grado);
            }
            if (dto.getTurno() != null) s.setTurno(dto.getTurno());
            if (dto.getCapacidadMaxima() != null) s.setCapacidadMaxima(dto.getCapacidadMaxima());
            if (dto.getActiva() != null) s.setActiva(dto.getActiva());
            if (dto.getDocenteTutorId() != null) s.setDocenteTutorId(dto.getDocenteTutorId());
            return toDTO(seccionRepository.save(s));
        });
    }

    public boolean eliminar(Long id) {
        if (seccionRepository.existsById(id)) {
            seccionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private SeccionDTO toDTO(Seccion s) {
        SeccionDTO dto = new SeccionDTO();
        dto.setId(s.getId());
        dto.setNombre(s.getNombre());
        dto.setGradoId(s.getGrado().getId());
        dto.setGradoNombre(s.getGrado().getNombre());
        dto.setNivelNombre(s.getGrado().getNivel().getNombre());
        dto.setTurno(s.getTurno());
        dto.setCapacidadMaxima(s.getCapacidadMaxima());
        dto.setActiva(s.getActiva());
        dto.setAnioEscolar(s.getAnioEscolar());
        dto.setDocenteTutorId(s.getDocenteTutorId());
        return dto;
    }
}
