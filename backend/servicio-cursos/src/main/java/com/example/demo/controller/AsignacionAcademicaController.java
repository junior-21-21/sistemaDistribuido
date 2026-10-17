package com.example.demo.controller;

import com.example.demo.dto.AsignacionAcademicaDTO;
import com.example.demo.service.AsignacionAcademicaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.security.Principal;
import java.util.Map;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/asignaciones-academicas")
@PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA', 'DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
public class AsignacionAcademicaController {
    private final AsignacionAcademicaService service;
    private final com.example.demo.service.RepartoCargaService reparto;

    public AsignacionAcademicaController(AsignacionAcademicaService service,
                                         com.example.demo.service.RepartoCargaService reparto) {
        this.service = service;
        this.reparto = reparto;
    }

    /** Reparte un curso entre varios docentes equilibrando su carga (simular=true solo muestra la propuesta). */
    @PostMapping("/reparto")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public com.example.demo.service.RepartoCargaService.Resultado repartir(
            @RequestBody com.example.demo.service.RepartoCargaService.Solicitud solicitud) {
        return reparto.repartir(solicitud);
    }

    @GetMapping
    public List<AsignacionAcademicaDTO> listar(@RequestParam(required = false) Long seccionId,
                                               @RequestParam(required = false) Integer anioEscolar,
                                               Principal principal,
                                               Authentication authentication) {
        if (requiereAlcanceDocente(authentication)) {
            return service.listarMias(principal.getName(), anioEscolar);
        }
        return service.listar(seccionId, anioEscolar);
    }

    @GetMapping("/mias")
    public List<AsignacionAcademicaDTO> listarMias(
            @RequestParam(required = false) Integer anioEscolar, Principal principal) {
        return service.listarMias(principal.getName(), anioEscolar);
    }

    @GetMapping("/mias/validar")
    public Map<String, Boolean> validarMia(
            @RequestParam Long cursoId,
            @RequestParam Long docenteId,
            @RequestParam Long seccionId,
            @RequestParam(required = false) Integer anioEscolar,
            Principal principal) {
        return Map.of("autorizada", service.validarMia(
                principal.getName(), cursoId, docenteId, seccionId, anioEscolar));
    }

    @GetMapping("/mias/secciones/{seccionId}/validar")
    public Map<String, Boolean> validarSeccionMia(
            @PathVariable Long seccionId,
            @RequestParam(required = false) Integer anioEscolar,
            Principal principal) {
        return Map.of("autorizada", service.validarSeccionMia(principal.getName(), seccionId, anioEscolar));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<AsignacionAcademicaDTO> crear(@Valid @RequestBody AsignacionAcademicaDTO dto) {
        return new ResponseEntity<>(service.guardar(null, dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public AsignacionAcademicaDTO actualizar(@PathVariable Long id, @Valid @RequestBody AsignacionAcademicaDTO dto) {
        return service.guardar(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private boolean requiereAlcanceDocente(Authentication authentication) {
        if (authentication == null || !tieneRol(authentication, "ROLE_DOCENTE")) return false;
        return !((tieneRol(authentication, "ROLE_DIRECTOR") || tieneRol(authentication, "ROLE_SUBDIRECTOR"))
                || tieneRol(authentication, "ROLE_SECRETARIA")
                || (tieneRol(authentication, "ROLE_DIRECTOR") || tieneRol(authentication, "ROLE_SUBDIRECTOR"))
                || tieneRol(authentication, "ROLE_AUXILIAR")
                || tieneRol(authentication, "ROLE_RESPONSABLE_CONVIVENCIA"));
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication.getAuthorities().stream().anyMatch(a -> rol.equals(a.getAuthority()));
    }
}
