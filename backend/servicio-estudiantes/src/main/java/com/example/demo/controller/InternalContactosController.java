package com.example.demo.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ContactoApoderadoDTO;
import com.example.demo.repository.ApoderadoEstudianteRepository;

@RestController
@RequestMapping("/api/internal/contactos")
public class InternalContactosController {

    private final ApoderadoEstudianteRepository repository;
    private final String expectedKey;

    public InternalContactosController(
            ApoderadoEstudianteRepository repository,
            @Value("${app.internal-api-key}") String expectedKey) {
        this.repository = repository;
        this.expectedKey = expectedKey;
    }

    @GetMapping("/estudiante/{estudianteId}")
    public List<ContactoApoderadoDTO> porEstudiante(
            @PathVariable Long estudianteId,
            @RequestHeader("X-Internal-Api-Key") String providedKey) {
        validarClave(providedKey);
        return repository.findByEstudianteId(estudianteId).stream().map(vinculo -> {
            var apoderado = vinculo.getApoderado();
            return new ContactoApoderadoDTO(apoderado.getId(), estudianteId,
                    apoderado.getNombre() + " " + apoderado.getApellido(), vinculo.getParentesco(),
                    apoderado.getEmail(), apoderado.getTelefono());
        }).toList();
    }

    private void validarClave(String providedKey) {
        boolean valida = MessageDigest.isEqual(expectedKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8));
        if (!valida) throw new org.springframework.security.access.AccessDeniedException("Clave interna inválida");
    }
}
