package com.example.demo.controller;

import com.example.demo.dto.PeriodoEscolarDTO;
import com.example.demo.service.PeriodoEscolarService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/periodos")
public class PeriodoEscolarController {

    private final PeriodoEscolarService periodoService;

    public PeriodoEscolarController(PeriodoEscolarService periodoService) {
        this.periodoService = periodoService;
    }

    @GetMapping
    public ResponseEntity<List<PeriodoEscolarDTO>> listar(
            @RequestParam(defaultValue = "0") Integer anio) {
        int anioEfectivo = anio == 0 ? Year.now().getValue() : anio;
        return ResponseEntity.ok(periodoService.listarPorAnio(anioEfectivo));
    }

    @GetMapping("/activo")
    public ResponseEntity<PeriodoEscolarDTO> getActivo(
            @RequestParam(defaultValue = "0") Integer anio) {
        int anioEfectivo = anio == 0 ? Year.now().getValue() : anio;
        return periodoService.buscarActivo(anioEfectivo)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PeriodoEscolarDTO> crear(@Valid @RequestBody PeriodoEscolarDTO dto) {
        return new ResponseEntity<>(periodoService.guardar(dto), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/suspender")
    public ResponseEntity<PeriodoEscolarDTO> suspender(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String motivo = body.getOrDefault("motivo", "Sin motivo especificado");
        LocalDate reanudacion = body.containsKey("fechaReanudacion")
            ? LocalDate.parse(body.get("fechaReanudacion")) : null;
        return periodoService.suspender(id, motivo, reanudacion)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/reanudar")
    public ResponseEntity<PeriodoEscolarDTO> reanudar(@PathVariable Long id) {
        return periodoService.reanudar(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
