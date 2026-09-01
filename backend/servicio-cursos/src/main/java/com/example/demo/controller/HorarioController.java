package com.example.demo.controller;

import com.example.demo.dto.HorarioDTO;
import com.example.demo.service.HorarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    @Autowired
    private HorarioService horarioService;

    @GetMapping("/seccion/{seccionId}")
    public ResponseEntity<List<HorarioDTO>> listarPorSeccion(@PathVariable Long seccionId) {
        return ResponseEntity.ok(horarioService.listarPorSeccion(seccionId));
    }

    @GetMapping("/docente/{docenteId}")
    public ResponseEntity<List<HorarioDTO>> listarPorDocente(@PathVariable Long docenteId) {
        return ResponseEntity.ok(horarioService.listarPorDocente(docenteId));
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody HorarioDTO horarioDTO) {
        try {
            return new ResponseEntity<>(horarioService.guardar(horarioDTO), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (horarioService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
