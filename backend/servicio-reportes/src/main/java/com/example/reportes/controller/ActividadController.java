package com.example.reportes.controller;

import com.example.reportes.model.Actividad;
import com.example.reportes.repository.ActividadRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/actividades")
public class ActividadController {

    private final ActividadRepository actividadRepository;

    public ActividadController(ActividadRepository actividadRepository) {
        this.actividadRepository = actividadRepository;
    }

    @PostMapping
    public ResponseEntity<Actividad> registrarActividad(@RequestBody Actividad actividad) {
        if (actividad.getFecha() == null) {
            actividad.setFecha(LocalDateTime.now());
        }
        Actividad guardada = actividadRepository.save(actividad);
        return ResponseEntity.ok(guardada);
    }

    @GetMapping("/recientes")
    public ResponseEntity<List<Actividad>> obtenerRecientes() {
        return ResponseEntity.ok(actividadRepository.findTop10ByOrderByFechaDesc());
    }
}
