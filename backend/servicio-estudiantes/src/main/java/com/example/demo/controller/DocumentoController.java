package com.example.demo.controller;

import com.example.demo.dto.DocumentoDTO;
import com.example.demo.service.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/estudiantes/{estudianteId}/documentos")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    @PostMapping
    public ResponseEntity<?> subirDocumento(
            @PathVariable Long estudianteId,
            @RequestParam("tipoDocumento") String tipoDocumento,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo no puede estar vacío");
        }
        
        try {
            DocumentoDTO dto = documentoService.guardarDocumento(estudianteId, tipoDocumento, file);
            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar el archivo: " + e.getMessage());
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
            org.springframework.core.io.Resource resource = documentoService.cargarArchivoComoRecurso(documentoId);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
