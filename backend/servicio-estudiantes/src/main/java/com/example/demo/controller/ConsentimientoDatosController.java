package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ConsentimientoDatosDTO;
import com.example.demo.dto.RegistrarConsentimientoRequest;
import com.example.demo.service.ConsentimientoDatosService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/consentimientos")
@PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR','SECRETARIA')")
public class ConsentimientoDatosController {
    private final ConsentimientoDatosService service;

    public ConsentimientoDatosController(ConsentimientoDatosService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConsentimientoDatosDTO> historial(@RequestParam Long estudianteId) {
        return service.historial(estudianteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SECRETARIA')")
    public ConsentimientoDatosDTO registrar(
            @Valid @RequestBody RegistrarConsentimientoRequest request, Principal principal) {
        return service.registrar(request, principal.getName());
    }
}
