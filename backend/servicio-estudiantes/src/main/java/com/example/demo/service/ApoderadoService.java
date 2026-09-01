package com.example.demo.service;

import com.example.demo.dto.ApoderadoDTO;
import com.example.demo.mapper.ApoderadoMapper;
import com.example.demo.model.Apoderado;
import com.example.demo.repository.ApoderadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApoderadoService {

    @Autowired
    private ApoderadoRepository apoderadoRepository;

    @Autowired
    private ApoderadoMapper apoderadoMapper;

    public List<ApoderadoDTO> listarTodos() {
        return apoderadoRepository.findAll().stream()
                .map(apoderadoMapper::toDto)
                .collect(Collectors.toList());
    }

    public Optional<ApoderadoDTO> buscarPorId(Long id) {
        return apoderadoRepository.findById(id).map(apoderadoMapper::toDto);
    }

    public ApoderadoDTO guardar(ApoderadoDTO apoderadoDTO) {
        // Validación extra de DNI duplicado podría ir aquí
        Apoderado apoderado = apoderadoMapper.toEntity(apoderadoDTO);
        apoderado = apoderadoRepository.save(apoderado);
        return apoderadoMapper.toDto(apoderado);
    }

    public Optional<ApoderadoDTO> actualizar(Long id, ApoderadoDTO detalles) {
        return apoderadoRepository.findById(id).map(apoderado -> {
            apoderadoMapper.updateEntityFromDto(detalles, apoderado);
            return apoderadoMapper.toDto(apoderadoRepository.save(apoderado));
        });
    }

    public boolean eliminar(Long id) {
        if (apoderadoRepository.existsById(id)) {
            apoderadoRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
