package com.example.reportes.controller;

import com.example.reportes.dto.AlertaDesercionDTO;
import com.example.reportes.service.AlertaDesercionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<List<AlertaDesercionDTO>>> getAlertasDesercion(
            @RequestParam(required = false) Long seccionId) {
        return alertaService.generarAlertas(seccionId).map(ResponseEntity::ok);
    }

    @GetMapping("/desercion/count")
    public Mono<ResponseEntity<Long>> countAlertasActivas() {
        return alertaService.contarAlertasActivas().map(ResponseEntity::ok);
    }
}
