package com.example.demo.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.NominaService;
import com.example.demo.service.NominaService.EstadoNomina;
import com.example.demo.service.NominaService.FilaNomina;

/** Nóminas de matrícula: Secretaría las revisa e imprime, Dirección las aprueba. */
@RestController
@RequestMapping("/api/nominas")
@PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA')")
public class NominaController {

    private final NominaService service;

    public NominaController(NominaService service) {
        this.service = service;
    }

    @GetMapping
    public List<EstadoNomina> estado(@RequestParam(required = false) Integer anio) {
        return service.estado(anio);
    }

    @GetMapping("/{seccionId}/estudiantes")
    public List<FilaNomina> detalle(@PathVariable Long seccionId) {
        return service.detalle(seccionId);
    }

    @PostMapping("/{seccionId}/aprobar")
    @PreAuthorize("hasRole('DIRECTOR')")
    public EstadoNomina aprobar(@PathVariable Long seccionId,
                                @RequestParam(required = false) Integer anio,
                                @RequestBody(required = false) Map<String, String> datos,
                                Principal principal) {
        return service.aprobar(seccionId, anio, datos == null ? null : datos.get("observacion"), principal.getName());
    }
}
