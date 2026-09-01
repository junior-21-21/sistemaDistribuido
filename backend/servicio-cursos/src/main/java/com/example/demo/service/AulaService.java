package com.example.demo.service;

import com.example.demo.dto.AulaDTO;
import com.example.demo.model.Aula;
import com.example.demo.repository.AulaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AulaService {

    private final AulaRepository aulaRepository;

    public AulaService(AulaRepository aulaRepository) {
        this.aulaRepository = aulaRepository;
    }

    public List<AulaDTO> listarActivas() {
        return aulaRepository.findByActivaTrue().stream()
            .map(this::toDTO).toList();
    }

    public List<AulaDTO> listarTodas() {
        return aulaRepository.findAll().stream()
            .map(this::toDTO).toList();
    }

    public Optional<AulaDTO> buscarPorId(Long id) {
        return aulaRepository.findById(id).map(this::toDTO);
    }

    public AulaDTO guardar(AulaDTO dto) {
        Aula aula = toEntity(dto);
        return toDTO(aulaRepository.save(aula));
    }

    public Optional<AulaDTO> actualizar(Long id, AulaDTO dto) {
        return aulaRepository.findById(id).map(a -> {
            a.setNombre(dto.getNombre());
            a.setPabellon(dto.getPabellon());
            a.setCapacidadMaxima(dto.getCapacidadMaxima());
            a.setTurnoDisponible(dto.getTurnoDisponible());
            a.setObservaciones(dto.getObservaciones());
            return toDTO(aulaRepository.save(a));
        });
    }

    public boolean eliminar(Long id) {
        if (aulaRepository.existsById(id)) {
            aulaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private AulaDTO toDTO(Aula a) {
        AulaDTO dto = new AulaDTO();
        dto.setId(a.getId());
        dto.setNombre(a.getNombre());
        dto.setPabellon(a.getPabellon());
        dto.setCapacidadMaxima(a.getCapacidadMaxima());
        dto.setTurnoDisponible(a.getTurnoDisponible());
        dto.setActiva(a.getActiva());
        dto.setObservaciones(a.getObservaciones());
        return dto;
    }

    private Aula toEntity(AulaDTO dto) {
        Aula a = new Aula();
        a.setNombre(dto.getNombre());
        a.setPabellon(dto.getPabellon());
        a.setCapacidadMaxima(dto.getCapacidadMaxima());
        a.setTurnoDisponible(dto.getTurnoDisponible() != null ? dto.getTurnoDisponible() : "AMBOS");
        a.setActiva(dto.getActiva() != null ? dto.getActiva() : true);
        a.setObservaciones(dto.getObservaciones());
        return a;
    }
}
