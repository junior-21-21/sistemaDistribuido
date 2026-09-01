package com.example.demo.controller;

import com.example.demo.model.DerivacionPsicologica;
import com.example.demo.repository.DerivacionPsicologicaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/derivaciones")
public class DerivacionController {

    @Autowired
    private DerivacionPsicologicaRepository derivacionRepository;

    @GetMapping
    public ResponseEntity<List<DerivacionPsicologica>> listarTodas() {
        return ResponseEntity.ok(derivacionRepository.findAll());
    }

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<DerivacionPsicologica>> listarPorEstudiante(@PathVariable Long estudianteId) {
        return ResponseEntity.ok(derivacionRepository.findByEstudianteId(estudianteId));
    }

    @GetMapping("/docente/{docenteId}")
    public ResponseEntity<List<DerivacionPsicologica>> listarPorDocente(@PathVariable Long docenteId) {
        return ResponseEntity.ok(derivacionRepository.findByDocenteDerivadorId(docenteId));
    }

    @PostMapping
    public ResponseEntity<DerivacionPsicologica> crearDerivacion(@RequestBody DerivacionPsicologica derivacion) {
        return new ResponseEntity<>(derivacionRepository.save(derivacion), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/atender")
    public ResponseEntity<DerivacionPsicologica> atenderDerivacion(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        
        return derivacionRepository.findById(id).map(deriv -> {
            String estado = payload.get("estado");
            String notas = payload.get("notasPsicologo");
            
            if (estado != null) deriv.setEstado(estado);
            if (notas != null) deriv.setNotasPsicologo(notas);
            if ("EN_EVALUACION".equals(estado) && deriv.getFechaAtencion() == null) {
                deriv.setFechaAtencion(LocalDateTime.now());
            }
            return ResponseEntity.ok(derivacionRepository.save(deriv));
        }).orElse(ResponseEntity.notFound().build());
    }
}
