package com.example.demo.controller;

import com.example.demo.dto.AsistenciaDTO;
import com.example.demo.service.AsistenciaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asistencias")
public class AsistenciaController {

    @Autowired
    private AsistenciaService asistenciaService;

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<AsistenciaDTO>> listarPorEstudiante(@PathVariable Long estudianteId) {
        return ResponseEntity.ok(asistenciaService.listarPorEstudiante(estudianteId));
    }

    @GetMapping("/horario/{horarioId}")
    public ResponseEntity<List<AsistenciaDTO>> listarPorHorario(@PathVariable Long horarioId) {
        return ResponseEntity.ok(asistenciaService.listarPorHorario(horarioId));
    }

    @PostMapping
    public ResponseEntity<AsistenciaDTO> crear(@Valid @RequestBody AsistenciaDTO asistenciaDTO) {
        return new ResponseEntity<>(asistenciaService.guardar(asistenciaDTO), HttpStatus.CREATED);
    }
}
