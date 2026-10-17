package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.CambioSeccion;
import com.example.demo.service.CambioSeccionService;
import com.example.demo.service.CambioSeccionService.SolicitudCambio;

/** Traslado interno de un estudiante a otra sección del mismo grado. */
@RestController
public class CambioSeccionController {

    private final CambioSeccionService service;

    public CambioSeccionController(CambioSeccionService service) {
        this.service = service;
    }

    @PostMapping("/api/estudiantes/{id}/cambio-seccion")
    @PreAuthorize("hasAnyRole('SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR')")
    public CambioSeccion cambiar(@PathVariable Long id, @RequestBody SolicitudCambio solicitud, Principal principal,
                                 @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return service.cambiar(id, solicitud, principal.getName(), authorization);
    }

    @GetMapping("/api/estudiantes/{id}/cambios-seccion")
    @PreAuthorize("hasAnyRole('SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR')")
    public List<CambioSeccion> historial(@PathVariable Long id) {
        return service.historial(id);
    }
}
