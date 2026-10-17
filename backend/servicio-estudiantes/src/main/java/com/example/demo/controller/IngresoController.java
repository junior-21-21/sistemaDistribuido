package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.IngresoDTO;
import com.example.demo.dto.ResumenIngresosDTO;
import com.example.demo.service.IngresoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Portería: registro de llegada de estudiantes escaneando su DNI. */
@RestController
@RequestMapping("/api/ingresos")
public class IngresoController {

    private final IngresoService service;

    public IngresoController(IngresoService service) {
        this.service = service;
    }

    public record RegistrarIngresoRequest(@NotBlank @Size(max = 400) String codigo, String metodo) {
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'PORTERO')")
    public ResponseEntity<IngresoDTO> registrar(@Valid @RequestBody RegistrarIngresoRequest request, Principal principal) {
        return ResponseEntity.ok(service.registrar(request.codigo(), request.metodo(), principal.getName()));
    }

    @GetMapping("/hoy")
    @PreAuthorize("hasAnyRole('AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'PORTERO', 'SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR', 'DOCENTE')")
    public ResumenIngresosDTO hoy() {
        return service.resumenDeHoy();
    }

    @GetMapping("/estudiante/{id}")
    @PreAuthorize("hasAnyRole('AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR')")
    public List<IngresoDTO> historial(@PathVariable Long id,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.historialEstudiante(id, desde, hasta);
    }
}
