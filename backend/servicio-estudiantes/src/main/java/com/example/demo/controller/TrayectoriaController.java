package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Matricula;
import com.example.demo.service.TrayectoriaService;
import com.example.demo.service.TrayectoriaService.AreaPendienteVista;
import com.example.demo.service.TrayectoriaService.Promocion;
import com.example.demo.service.TrayectoriaService.ResumenPromocion;
import com.example.demo.service.TrayectoriaService.Trayectoria;
import com.example.demo.service.TrayectoriaService.TrasladoSalida;

/** Matrícula por año, traslados, promoción de grado y recuperación pedagógica. */
@RestController
public class TrayectoriaController {

    private final TrayectoriaService service;

    public TrayectoriaController(TrayectoriaService service) {
        this.service = service;
    }

    public record ResultadoRecuperacion(String estado, String calificacion, String observacion) {
    }

    @GetMapping("/api/estudiantes/{id}/trayectoria")
    @PreAuthorize("hasAnyRole('SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR')")
    public Trayectoria trayectoria(@PathVariable Long id) {
        return service.trayectoria(id);
    }

    @PostMapping("/api/estudiantes/{id}/traslado-salida")
    @PreAuthorize("hasAnyRole('SECRETARIA','DIRECTOR')")
    public Matricula trasladar(@PathVariable Long id, @RequestBody TrasladoSalida datos, Principal principal) {
        return service.trasladarSalida(id, datos, principal.getName());
    }

    @PostMapping("/api/estudiantes/promocion")
    @PreAuthorize("hasAnyRole('SECRETARIA','DIRECTOR', 'SUBDIRECTOR')")
    public ResumenPromocion promover(@RequestBody Promocion promocion, Principal principal) {
        return service.promover(promocion, principal.getName());
    }

    @GetMapping("/api/recuperacion")
    @PreAuthorize("hasAnyRole('SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR', 'DOCENTE')")
    public List<AreaPendienteVista> areas(@RequestParam(required = false) String estado,
                                          @RequestParam(required = false) String tipo) {
        return service.listarAreas(estado, tipo);
    }

    /** Cierra la recuperación del estudiante: promovido, promovido con un área por subsanar o permanece. */
    @PostMapping("/api/recuperacion/estudiante/{id}/cerrar")
    @PreAuthorize("hasAnyRole('SECRETARIA','DIRECTOR', 'SUBDIRECTOR')")
    public TrayectoriaService.ResultadoCierre cerrar(@PathVariable Long id,
                                                     @RequestBody TrayectoriaService.CierreRecuperacion datos,
                                                     Principal principal) {
        return service.cerrarRecuperacion(id, datos, principal.getName());
    }

    @PutMapping("/api/recuperacion/{id}")
    @PreAuthorize("hasAnyRole('SECRETARIA', 'DIRECTOR', 'SUBDIRECTOR')")
    public AreaPendienteVista resultado(@PathVariable Long id, @RequestBody ResultadoRecuperacion datos, Principal principal) {
        return service.registrarResultado(id, datos.estado(), datos.calificacion(), datos.observacion(), principal.getName());
    }
}
