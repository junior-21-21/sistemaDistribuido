package com.example.demo.controller;

import com.example.demo.dto.NotaDTO;
import com.example.demo.service.NotaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notas")
public class NotaController {

    @Autowired
    private NotaService notaService;

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<NotaDTO>> listarPorEstudiante(@PathVariable Long estudianteId) {
        return ResponseEntity.ok(notaService.listarPorEstudiante(estudianteId));
    }

    @GetMapping("/curso/{cursoId}")
    public ResponseEntity<List<NotaDTO>> listarPorCurso(@PathVariable Long cursoId) {
        return ResponseEntity.ok(notaService.listarPorCurso(cursoId));
    }

    @PostMapping
    public ResponseEntity<NotaDTO> crear(@Valid @RequestBody NotaDTO notaDTO) {
        return new ResponseEntity<>(notaService.guardar(notaDTO), HttpStatus.CREATED);
    }
}
