package com.example.demo.controller;

import com.example.demo.dto.DocumentoDTO;
import com.example.demo.service.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.example.demo.service.DocumentoService.DocumentoDescarga;

@RestController
@RequestMapping("/api/estudiantes/{estudianteId}/documentos")
@PreAuthorize("hasRole('SECRETARIA')")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    @PostMapping
    public ResponseEntity<?> subirDocumento(
            @PathVariable Long estudianteId,
            @RequestParam("tipoDocumento") String tipoDocumento,
            @RequestParam("file") MultipartFile file) {
        
        try {
            DocumentoDTO dto = documentoService.guardarDocumento(estudianteId, tipoDocumento, file);
            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("No se pudo almacenar el archivo");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<DocumentoDTO>> listarDocumentos(@PathVariable Long estudianteId) {
        return ResponseEntity.ok(documentoService.listarDocumentosPorEstudiante(estudianteId));
    }

    @GetMapping("/{documentoId}/descargar")
    public ResponseEntity<org.springframework.core.io.Resource> descargarDocumento(@PathVariable Long estudianteId, @PathVariable Long documentoId) {
        try {
            DocumentoDescarga descarga = documentoService.cargarArchivoComoRecurso(estudianteId, documentoId);
            String disposition = ContentDisposition.attachment()
                    .filename(descarga.nombreArchivo(), StandardCharsets.UTF_8)
                    .build()
                    .toString();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .contentType(MediaType.parseMediaType(descarga.contentType()))
                    .body(descarga.resource());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
