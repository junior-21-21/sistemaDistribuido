package com.example.demo.controller;

import com.example.demo.dto.SeccionDTO;
import com.example.demo.service.SeccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/secciones")
public class SeccionController {

    private final SeccionService seccionService;

    public SeccionController(SeccionService seccionService) {
        this.seccionService = seccionService;
    }

    @GetMapping
    public ResponseEntity<List<SeccionDTO>> listar(
            @RequestParam(required = false) Long gradoId,
            @RequestParam(required = false) Integer anioEscolar) {
        if (gradoId != null) {
            return ResponseEntity.ok(seccionService.listarPorGrado(gradoId));
        }
        if (anioEscolar != null) {
            return ResponseEntity.ok(seccionService.listarPorAnio(anioEscolar));
        }
        return ResponseEntity.ok(seccionService.listarPorAnio(java.time.Year.now().getValue()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeccionDTO> obtener(@PathVariable Long id) {
        return seccionService.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SeccionDTO> crear(@Valid @RequestBody SeccionDTO dto) {
        return new ResponseEntity<>(seccionService.guardar(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeccionDTO> actualizar(@PathVariable Long id, @Valid @RequestBody SeccionDTO dto) {
        return seccionService.actualizar(id, dto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return seccionService.eliminar(id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }
}
