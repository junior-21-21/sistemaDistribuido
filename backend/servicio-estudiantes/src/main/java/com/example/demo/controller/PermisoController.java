package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CrearPermisoRequest;
import com.example.demo.dto.PermisoDTO;
import com.example.demo.dto.RevisarPermisoRequest;
import com.example.demo.service.PermisoService;

import jakarta.validation.Valid;

/** Justificaciones de inasistencia, papeletas de salida y permisos registrados por el personal. */
@RestController
@RequestMapping("/api/permisos")
@PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA', 'DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'PORTERO')")
public class PermisoController {

    private final PermisoService service;

    public PermisoController(PermisoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA', 'DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public List<PermisoDTO> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listar(estado, desde, hasta);
    }

    @GetMapping("/salidas-hoy")
    public List<PermisoDTO> salidasDeHoy() {
        return service.salidasDeHoy();
    }

    @GetMapping("/pendientes/total")
    public Map<String, Long> pendientes() {
        return Map.of("pendientes", service.pendientes());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA', 'DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public ResponseEntity<PermisoDTO> registrar(@Valid @RequestBody CrearPermisoRequest request,
                                                Principal principal, Authentication authentication) {
        return new ResponseEntity<>(service.registrarPorPersonal(request, principal.getName(), roles(authentication)),
                HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/revision")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public PermisoDTO revisar(@PathVariable Long id, @Valid @RequestBody RevisarPermisoRequest request,
                              Principal principal, Authentication authentication) {
        return service.revisar(id, request.aprobar(), request.observacion(), principal.getName(), roles(authentication));
    }

    @PatchMapping("/{id}/salida")
    @PreAuthorize("hasAnyRole('PORTERO', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public PermisoDTO registrarSalida(@PathVariable Long id, Principal principal, Authentication authentication) {
        return service.registrarSalida(id, principal.getName(), roles(authentication));
    }

    static Set<String> roles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.toSet());
    }
}
