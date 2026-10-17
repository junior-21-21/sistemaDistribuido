package com.example.demo.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuditoriaDTO;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.AuditoriaService;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('DIRECTOR')")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AuditoriaDTO> buscar(
            @RequestParam String entidad,
            @RequestParam(required = false) Long entidadId,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @PageableDefault(size = 25) Pageable pageable) {
        return service.buscar(entidad, entidadId, usuario, desde, hasta, pageable);
    }
}
