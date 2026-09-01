package com.example.demo.controller;

import com.example.demo.dto.NivelDTO;
import com.example.demo.service.NivelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/niveles")
public class NivelController {

    private final NivelService nivelService;

    public NivelController(NivelService nivelService) {
        this.nivelService = nivelService;
    }

    @GetMapping
    public ResponseEntity<List<NivelDTO>> listar() {
        return ResponseEntity.ok(nivelService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NivelDTO> obtener(@PathVariable Long id) {
        return nivelService.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<NivelDTO> crear(@Valid @RequestBody NivelDTO dto) {
        return new ResponseEntity<>(nivelService.guardar(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NivelDTO> actualizar(@PathVariable Long id, @Valid @RequestBody NivelDTO dto) {
        return nivelService.actualizar(id, dto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return nivelService.eliminar(id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }
}
