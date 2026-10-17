package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import com.example.demo.dto.AtencionMedicaDTO;
import com.example.demo.dto.CrearAtencionMedicaRequest;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.AtencionMedicaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/atenciones-medicas")
@PreAuthorize("hasAnyRole('ENFERMERA', 'PRIMEROS_AUXILIOS', 'DIRECTOR', 'SUBDIRECTOR')")
public class AtencionMedicaController {

    private final AtencionMedicaService service;

    public AtencionMedicaController(AtencionMedicaService service) {
        this.service = service;
    }

    @GetMapping("/ficha/{fichaId}")
    public List<AtencionMedicaDTO> listarPorFicha(@PathVariable Long fichaId) {
        return service.listarPorFicha(fichaId);
    }

    @GetMapping("/ficha/{fichaId}/page")
    public PageResponse<AtencionMedicaDTO> listarPorFichaPaginado(
            @PathVariable Long fichaId,
            @PageableDefault(size = 10, sort = "fechaAtencion", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.from(service.listarPorFicha(fichaId, pageable));
    }

    @PostMapping
    public ResponseEntity<AtencionMedicaDTO> registrar(
            @Valid @RequestBody CrearAtencionMedicaRequest request,
            Principal principal) {
        return new ResponseEntity<>(service.registrar(request, principal.getName()), HttpStatus.CREATED);
    }
}
