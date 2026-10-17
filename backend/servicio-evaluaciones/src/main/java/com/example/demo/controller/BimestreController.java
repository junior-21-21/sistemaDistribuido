package com.example.demo.controller;

import java.security.Principal;
import java.time.Year;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.BimestreAuditoriaDTO;
import com.example.demo.dto.BimestreEstadoDTO;
import com.example.demo.dto.CambiarEstadoBimestreRequest;
import com.example.demo.service.BimestreService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bimestres")
@PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'DOCENTE')")
public class BimestreController {
    private final BimestreService service;

    public BimestreController(BimestreService service) {
        this.service = service;
    }

    @GetMapping
    public List<BimestreEstadoDTO> listar(
            @RequestParam(required = false) Integer anio) {
        return service.listar(anio == null ? Year.now().getValue() : anio);
    }

    @GetMapping("/{anio}/{bimestre}/auditoria")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public List<BimestreAuditoriaDTO> auditoria(
            @PathVariable int anio, @PathVariable String bimestre) {
        return service.auditoria(anio, bimestre);
    }

    @PostMapping("/{anio}/{bimestre}/cerrar")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public BimestreEstadoDTO cerrar(@PathVariable int anio, @PathVariable String bimestre,
                                    @Valid @RequestBody CambiarEstadoBimestreRequest request,
                                    Principal principal) {
        return service.cerrar(anio, bimestre, request.motivo(), principal.getName());
    }

    @PostMapping("/{anio}/{bimestre}/reabrir")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public BimestreEstadoDTO reabrir(@PathVariable int anio, @PathVariable String bimestre,
                                     @Valid @RequestBody CambiarEstadoBimestreRequest request,
                                     Principal principal) {
        return service.reabrir(anio, bimestre, request.motivo(), principal.getName());
    }
}
