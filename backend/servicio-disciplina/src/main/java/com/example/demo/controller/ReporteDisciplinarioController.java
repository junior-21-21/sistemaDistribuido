package com.example.demo.controller;

import com.example.demo.dto.ReporteDisciplinarioDTO;
import com.example.demo.service.ActividadClient;
import com.example.demo.service.AsignacionAcademicaClient;
import com.example.demo.service.ReporteDisciplinarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpHeaders;

import java.security.Principal;
import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes-disciplinarios")
@PreAuthorize("hasAnyRole('DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA', 'DIRECTOR', 'SUBDIRECTOR')")
public class ReporteDisciplinarioController {

    private final ReporteDisciplinarioService service;
    private final ActividadClient actividadClient;
    private final AsignacionAcademicaClient asignacionClient;

    public ReporteDisciplinarioController(
            ReporteDisciplinarioService service,
            ActividadClient actividadClient,
            AsignacionAcademicaClient asignacionClient) {
        this.service = service;
        this.actividadClient = actividadClient;
        this.asignacionClient = asignacionClient;
    }

    @GetMapping
    public ResponseEntity<List<ReporteDisciplinarioDTO>> listar(
            @RequestParam(required = false) Long seccionId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        if (requiereAsignacionPropia(authentication)) {
            if (seccionId == null) {
                throw new IllegalArgumentException("El docente debe seleccionar una sección asignada");
            }
            asignacionClient.validarSeccionPropia(authorization, seccionId, Year.now().getValue());
        }
        return ResponseEntity.ok(service.listar(seccionId));
    }

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<ReporteDisciplinarioDTO>> listarPorEstudiante(
            @PathVariable Long estudianteId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        List<ReporteDisciplinarioDTO> reportes = service.listarPorEstudiante(estudianteId);
        if (requiereAsignacionPropia(authentication)) {
            List<Long> secciones = asignacionClient.seccionesPropias(authorization, Year.now().getValue());
            reportes = reportes.stream().filter(item -> secciones.contains(item.getSeccionId())).toList();
        }
        return ResponseEntity.ok(reportes);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public ResponseEntity<ReporteDisciplinarioDTO> crear(
            @Valid @RequestBody ReporteDisciplinarioDTO dto,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Principal principal,
            Authentication authentication) {
        if (requiereAsignacionPropia(authentication)) {
            asignacionClient.validarSeccionPropia(
                    authorization, dto.getSeccionId(), Year.now().getValue());
        } else {
            dto.setDocenteId(null);
        }
        ReporteDisciplinarioDTO guardado = service.guardar(dto, principal.getName());
        actividadClient.registrarPapeleta(guardado);
        return new ResponseEntity<>(guardado, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('RESPONSABLE_CONVIVENCIA')")
    public ResponseEntity<ReporteDisciplinarioDTO> actualizarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(service.actualizarEstado(id, payload.get("estado")));
    }

    @PatchMapping("/{id}/protocolo")
    @PreAuthorize("hasAnyRole('RESPONSABLE_CONVIVENCIA', 'DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<ReporteDisciplinarioDTO> actualizarProtocolo(
            @PathVariable Long id,
            @Valid @RequestBody com.example.demo.dto.ProtocoloViolenciaRequest datos,
            Principal principal) {
        return ResponseEntity.ok(service.actualizarProtocolo(id, datos, principal.getName()));
    }

    private boolean requiereAsignacionPropia(Authentication authentication) {
        boolean docente = tieneRol(authentication, "ROLE_DOCENTE");
        boolean coberturaConvivencia = tieneRol(authentication, "ROLE_AUXILIAR")
                || tieneRol(authentication, "ROLE_RESPONSABLE_CONVIVENCIA");
        return docente && !coberturaConvivencia;
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> rol.equals(a.getAuthority()));
    }
}
