package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CrearPermisoRequest;
import com.example.demo.dto.PermisoDTO;
import com.example.demo.service.PermisoService;

import jakarta.validation.Valid;

/** El apoderado pide justificaciones, papeletas de salida o permisos para sus hijos desde el portal. */
@RestController
@RequestMapping("/api/portal-apoderado/permisos")
@PreAuthorize("hasRole('APODERADO')")
public class PortalPermisoController {

    private final PermisoService service;

    public PortalPermisoController(PermisoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PermisoDTO> listar(Principal principal) {
        return service.listarDeApoderado(principal.getName());
    }

    @PostMapping
    public ResponseEntity<PermisoDTO> solicitar(@Valid @RequestBody CrearPermisoRequest request, Principal principal) {
        return new ResponseEntity<>(service.registrarPorApoderado(request, principal.getName()), HttpStatus.CREATED);
    }
}
