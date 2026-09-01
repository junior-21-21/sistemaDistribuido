package com.example.demo.controller;

import com.example.demo.dto.AulaDTO;
import com.example.demo.service.AulaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/aulas")
public class AulaController {

    private final AulaService aulaService;

    public AulaController(AulaService aulaService) {
        this.aulaService = aulaService;
    }

    @GetMapping
    public ResponseEntity<List<AulaDTO>> listar() {
        return ResponseEntity.ok(aulaService.listarActivas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AulaDTO> obtener(@PathVariable Long id) {
        return aulaService.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AulaDTO> crear(@Valid @RequestBody AulaDTO dto) {
        return new ResponseEntity<>(aulaService.guardar(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AulaDTO> actualizar(@PathVariable Long id, @Valid @RequestBody AulaDTO dto) {
        return aulaService.actualizar(id, dto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return aulaService.eliminar(id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }
}
