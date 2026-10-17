package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.FichaMedicaRequest;
import com.example.demo.model.FichaMedica;
import com.example.demo.service.FichaMedicaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/fichas-medicas")
@PreAuthorize("hasAnyRole('ENFERMERA', 'PRIMEROS_AUXILIOS', 'DIRECTOR', 'SUBDIRECTOR')")
public class FichaMedicaController {

    private final FichaMedicaService service;

    public FichaMedicaController(FichaMedicaService service) {
        this.service = service;
    }

    @GetMapping
    public List<FichaMedica> listarTodas() {
        return service.listarTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FichaMedica> obtenerPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<FichaMedica> obtenerPorEstudiante(@PathVariable Long estudianteId) {
        return service.buscarPorEstudiante(estudianteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FichaMedica> crear(
            @Valid @RequestBody FichaMedicaRequest request,
            Principal principal) {
        return new ResponseEntity<>(service.crear(request, principal.getName()), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FichaMedica> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody FichaMedicaRequest request,
            Principal principal) {
        return service.actualizar(id, request, principal.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
