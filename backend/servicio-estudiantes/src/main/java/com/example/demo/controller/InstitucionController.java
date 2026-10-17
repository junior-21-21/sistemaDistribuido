package com.example.demo.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ModuloInstitucionService;

/** Configuración del colegio: módulos opcionales según el personal con que cuenta. */
@RestController
@RequestMapping("/api/institucion")
public class InstitucionController {

    private final ModuloInstitucionService service;

    public InstitucionController(ModuloInstitucionService service) {
        this.service = service;
    }

    @GetMapping("/modulos")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Boolean> modulos() {
        return service.estado();
    }

    @PutMapping("/modulos/{codigo}")
    @PreAuthorize("hasAnyRole('DIRECTOR')")
    public Map<String, Boolean> cambiar(@PathVariable String codigo, @RequestBody Map<String, Boolean> cuerpo,
                                        Principal principal) {
        return service.cambiar(codigo, Boolean.TRUE.equals(cuerpo.get("habilitado")), principal.getName());
    }
}
