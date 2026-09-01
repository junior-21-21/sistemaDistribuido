package com.example.demo.controller;

import com.example.demo.dto.GradoDTO;
import com.example.demo.service.GradoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/grados")
public class GradoController {

    private final GradoService gradoService;

    public GradoController(GradoService gradoService) {
        this.gradoService = gradoService;
    }

    @GetMapping
    public ResponseEntity<List<GradoDTO>> listar(
            @RequestParam(required = false) Long nivelId) {
        if (nivelId != null) {
            return ResponseEntity.ok(gradoService.listarPorNivel(nivelId));
        }
        return ResponseEntity.ok(gradoService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GradoDTO> obtener(@PathVariable Long id) {
        return gradoService.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<GradoDTO> crear(@Valid @RequestBody GradoDTO dto) {
        return new ResponseEntity<>(gradoService.guardar(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GradoDTO> actualizar(@PathVariable Long id, @Valid @RequestBody GradoDTO dto) {
        return gradoService.actualizar(id, dto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return gradoService.eliminar(id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }
}
