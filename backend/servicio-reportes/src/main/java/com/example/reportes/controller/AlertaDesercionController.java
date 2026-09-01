package com.example.reportes.controller;

import com.example.reportes.dto.AlertaDesercionDTO;
import com.example.reportes.service.AlertaDesercionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Alertas tempranas de riesgo de desercion escolar.
 * Reglas: >3 faltas en las ultimas 2 semanas O promedio < B (escala literal).
 */
@RestController
@RequestMapping("/api/alertas")
public class AlertaDesercionController {

    private final AlertaDesercionService alertaService;

    public AlertaDesercionController(AlertaDesercionService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping("/desercion")
    public ResponseEntity<List<AlertaDesercionDTO>> getAlertasDesercion(
            @RequestParam(required = false) Long seccionId) {
        return ResponseEntity.ok(alertaService.generarAlertas(seccionId));
    }

    @GetMapping("/desercion/count")
    public ResponseEntity<Long> countAlertasActivas() {
        return ResponseEntity.ok(alertaService.contarAlertasActivas());
    }
}
