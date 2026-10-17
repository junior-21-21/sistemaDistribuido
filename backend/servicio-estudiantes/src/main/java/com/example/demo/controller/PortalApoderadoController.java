package com.example.demo.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.PortalEstudianteDTO;
import com.example.demo.dto.PortalResumenDTO;
import com.example.demo.service.DocumentoService.DocumentoDescarga;
import com.example.demo.service.PortalApoderadoService;

@RestController
@RequestMapping("/api/portal-apoderado")
@PreAuthorize("hasRole('APODERADO')")
public class PortalApoderadoController {

    private final PortalApoderadoService service;

    public PortalApoderadoController(PortalApoderadoService service) {
        this.service = service;
    }

    @GetMapping("/estudiantes")
    public List<PortalEstudianteDTO> estudiantes(Principal principal) {
        return service.listarEstudiantes(principal.getName());
    }

    @GetMapping("/estudiantes/{estudianteId}/resumen")
    public PortalResumenDTO resumen(@PathVariable Long estudianteId, Principal principal) {
        return service.obtenerResumen(principal.getName(), estudianteId);
    }

    @GetMapping("/estudiantes/{estudianteId}/documentos/{documentoId}/descargar")
    public ResponseEntity<Resource> descargar(
            @PathVariable Long estudianteId,
            @PathVariable Long documentoId,
            Principal principal) throws IOException {
        DocumentoDescarga descarga = service.descargar(principal.getName(), estudianteId, documentoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(descarga.nombreArchivo(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(descarga.contentType()))
                .body(descarga.resource());
    }
}
