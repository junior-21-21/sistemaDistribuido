package com.example.reportes.controller;

import java.util.List;
import java.util.Map;

import com.example.reportes.service.ReconciliacionIntegridadService;
import com.example.reportes.service.ReconciliacionIntegridadService.ResultadoReconciliacion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integridad")
@PreAuthorize("hasAnyRole('DIRECTOR')")
public class IntegridadController {
    private final ReconciliacionIntegridadService service;

    public IntegridadController(ReconciliacionIntegridadService service) {
        this.service = service;
    }

    @PostMapping("/reconciliar")
    public ResponseEntity<ResultadoReconciliacion> reconciliar() {
        return ResponseEntity.ok(service.ejecutar());
    }

    @GetMapping("/incidencias")
    public ResponseEntity<List<Map<String, Object>>> incidencias(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.listarIncidencias(estado));
    }

    @GetMapping("/ejecuciones")
    public ResponseEntity<List<Map<String, Object>>> ejecuciones() {
        return ResponseEntity.ok(service.listarEjecuciones());
    }
}
