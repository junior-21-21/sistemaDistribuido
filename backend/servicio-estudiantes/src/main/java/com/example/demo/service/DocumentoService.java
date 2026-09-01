package com.example.demo.service;

import com.example.demo.dto.DocumentoDTO;
import com.example.demo.model.Documento;
import com.example.demo.model.Estudiante;
import com.example.demo.repository.DocumentoRepository;
import com.example.demo.repository.EstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentoService {

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    private final String UPLOAD_DIR = "uploads/documentos/";

    public DocumentoService() {
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public DocumentoDTO guardarDocumento(Long estudianteId, String tipoDocumento, MultipartFile file) throws IOException {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = "EST_" + estudianteId + "_" + tipoDocumento + "_" + System.currentTimeMillis() + extension;
        
        Path filePath = Paths.get(UPLOAD_DIR + newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Documento doc = new Documento();
        doc.setEstudiante(estudiante);
        doc.setNombreArchivo(originalFilename);
        doc.setTipoDocumento(tipoDocumento);
        doc.setRutaLocal(filePath.toString());
        
        doc = documentoRepository.save(doc);
        return toDto(doc);
    }

    public List<DocumentoDTO> listarDocumentosPorEstudiante(Long estudianteId) {
        return documentoRepository.findByEstudianteId(estudianteId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private DocumentoDTO toDto(Documento doc) {
        DocumentoDTO dto = new DocumentoDTO();
        dto.setId(doc.getId());
        dto.setNombreArchivo(doc.getNombreArchivo());
        dto.setTipoDocumento(doc.getTipoDocumento());
        dto.setFechaSubida(doc.getFechaSubida());
        dto.setEstudianteId(doc.getEstudiante().getId());
        return dto;
    }

    public org.springframework.core.io.Resource cargarArchivoComoRecurso(Long documentoId) throws Exception {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
        Path filePath = Paths.get(doc.getRutaLocal());
        org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new Exception("No se pudo leer el archivo: " + doc.getNombreArchivo());
        }
    }
}
