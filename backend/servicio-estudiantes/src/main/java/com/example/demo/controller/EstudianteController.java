package com.example.demo.controller;

import com.example.demo.dto.EstudianteDTO;
import com.example.demo.service.EstudianteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/estudiantes")
public class EstudianteController {

    @Autowired
    private EstudianteService estudianteService;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public ResponseEntity<List<EstudianteDTO>> listarEstudiantes() {
        return ResponseEntity.ok(estudianteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstudianteDTO> obtenerEstudiante(@PathVariable Long id) {
        return estudianteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EstudianteDTO> crearEstudiante(@Valid @RequestBody EstudianteDTO estudianteDTO) {
        EstudianteDTO nuevoEstudiante = estudianteService.guardar(estudianteDTO);
        
        try {
            Map<String, String> actividad = new HashMap<>();
            actividad.put("texto", nuevoEstudiante.getNombre() + " " + nuevoEstudiante.getApellido() + " fue matriculado");
            actividad.put("tipo", "success");
            actividad.put("origen", "ESTUDIANTES");

            restTemplate.postForObject("http://servicio-reportes:8085/api/actividades", actividad, Void.class);
        } catch (Exception e) {
            System.err.println("Error al notificar actividad: " + e.getMessage());
        }

        return new ResponseEntity<>(nuevoEstudiante, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstudianteDTO> actualizarEstudiante(@PathVariable Long id, @Valid @RequestBody EstudianteDTO detalles) {
        return estudianteService.actualizar(id, detalles)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEstudiante(@PathVariable Long id) {
        if (estudianteService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/count/total")
    public ResponseEntity<Long> countTotal() {
        return ResponseEntity.ok(estudianteService.countTotal());
    }

    @GetMapping("/count/pendientes")
    public ResponseEntity<Long> countPendientes() {
        return ResponseEntity.ok(estudianteService.countPendientes());
    }
}
