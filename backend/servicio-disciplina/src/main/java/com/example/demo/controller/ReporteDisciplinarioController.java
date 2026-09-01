package com.example.demo.controller;

import com.example.demo.dto.ReporteDisciplinarioDTO;
import com.example.demo.service.ReporteDisciplinarioService;
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
@RequestMapping("/api/disciplina/reportes")
public class ReporteDisciplinarioController {

    @Autowired
    private ReporteDisciplinarioService service;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<ReporteDisciplinarioDTO>> listarPorEstudiante(@PathVariable Long estudianteId) {
        return ResponseEntity.ok(service.listarPorEstudiante(estudianteId));
    }

    @PostMapping
    public ResponseEntity<ReporteDisciplinarioDTO> crear(@Valid @RequestBody ReporteDisciplinarioDTO dto) {
        ReporteDisciplinarioDTO guardado = service.guardar(dto);

        try {
            Map<String, String> actividad = new HashMap<>();
            actividad.put("texto", "Se emitió una papeleta para el estudiante ID: " + guardado.getEstudianteId() + " por " + guardado.getTipo());
            actividad.put("tipo", "warning");
            actividad.put("origen", "DISCIPLINA");

            restTemplate.postForObject("http://servicio-reportes:8085/api/actividades", actividad, Void.class);
        } catch (Exception e) {
            System.err.println("Error al notificar actividad: " + e.getMessage());
        }

        return new ResponseEntity<>(guardado, HttpStatus.CREATED);
    }
}
