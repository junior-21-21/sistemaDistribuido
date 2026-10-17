package com.example.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.DocumentoDTO;
import com.example.demo.model.Documento;
import com.example.demo.model.Estudiante;
import com.example.demo.repository.DocumentoRepository;
import com.example.demo.repository.EstudianteRepository;

@Service
@Transactional
public class DocumentoService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> TIPOS_DOCUMENTO = Set.of(
            "DNI", "PARTIDA_NACIMIENTO", "RESOLUCION_SIAGIE", "CERTIFICADO_MEDICO");
    private static final Map<String, String> EXTENSION_POR_MIME = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png");

    private final DocumentoRepository documentoRepository;
    private final EstudianteRepository estudianteRepository;
    private final Path uploadDir;

    public DocumentoService(
            DocumentoRepository documentoRepository,
            EstudianteRepository estudianteRepository,
            @Value("${app.upload-dir:uploads/documentos}") String uploadDirectory) {
        this.documentoRepository = documentoRepository;
        this.estudianteRepository = estudianteRepository;
        this.uploadDir = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo inicializar el directorio de documentos", e);
        }
    }

    public DocumentoDTO guardarDocumento(Long estudianteId, String tipoDocumento, MultipartFile file) throws IOException {
        validarArchivo(file);
        String tipoNormalizado = normalizarTipo(tipoDocumento);
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));

        String contentType = normalizarContentType(file.getContentType());
        String extension = EXTENSION_POR_MIME.get(contentType);
        validarFirma(file, contentType);
        String nombreOriginal = limpiarNombreOriginal(file.getOriginalFilename(), extension);
        Path filePath = uploadDir.resolve(UUID.randomUUID() + extension).normalize();
        verificarRutaSegura(filePath);

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, filePath);
        }

        try {
            Documento documento = new Documento();
            documento.setEstudiante(estudiante);
            documento.setNombreArchivo(nombreOriginal);
            documento.setTipoDocumento(tipoNormalizado);
            documento.setRutaLocal(filePath.toString());
            return toDto(documentoRepository.save(documento));
        } catch (RuntimeException e) {
            Files.deleteIfExists(filePath);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentoDTO> listarDocumentosPorEstudiante(Long estudianteId) {
        return documentoRepository.findByEstudianteId(estudianteId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentoDescarga cargarArchivoComoRecurso(Long estudianteId, Long documentoId) throws IOException {
        Documento documento = documentoRepository.findByIdAndEstudianteId(documentoId, estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado para el estudiante"));
        Path filePath = Paths.get(documento.getRutaLocal()).toAbsolutePath().normalize();
        verificarRutaSegura(filePath);
        Resource resource = new UrlResource(filePath.toUri());
        if (!Files.isRegularFile(filePath) || !resource.isReadable()) {
            throw new IOException("El archivo solicitado no está disponible");
        }
        String contentType = Files.probeContentType(filePath);
        return new DocumentoDescarga(resource, documento.getNombreArchivo(),
                contentType == null ? "application/octet-stream" : contentType);
    }

    private void validarArchivo(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo supera el límite de 10 MB");
        }
        String contentType = normalizarContentType(file.getContentType());
        if (!EXTENSION_POR_MIME.containsKey(contentType)) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF, JPEG o PNG");
        }
    }

    private String normalizarTipo(String value) {
        String tipo = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_DOCUMENTO.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de documento inválido");
        }
        return tipo;
    }

    private String normalizarContentType(String value) {
        if (value == null) return "";
        return value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private void validarFirma(MultipartFile file, String contentType) throws IOException {
        byte[] firma;
        try (InputStream input = file.getInputStream()) {
            firma = input.readNBytes(8);
        }
        boolean valida = switch (contentType) {
            case "application/pdf" -> empiezaCon(firma, new int[] {0x25, 0x50, 0x44, 0x46});
            case "image/jpeg" -> empiezaCon(firma, new int[] {0xFF, 0xD8, 0xFF});
            case "image/png" -> empiezaCon(firma, new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            default -> false;
        };
        if (!valida) {
            throw new IllegalArgumentException("El contenido del archivo no coincide con su formato declarado");
        }
    }

    private boolean empiezaCon(byte[] contenido, int[] firma) {
        if (contenido.length < firma.length) return false;
        for (int i = 0; i < firma.length; i++) {
            if (Byte.toUnsignedInt(contenido[i]) != firma[i]) return false;
        }
        return true;
    }

    private String limpiarNombreOriginal(String value, String extension) {
        if (value == null || value.isBlank()) return "documento" + extension;
        String nombre = Paths.get(value).getFileName().toString().replaceAll("[\\r\\n]", "_");
        return nombre.length() <= 240 ? nombre : nombre.substring(0, 240);
    }

    private void verificarRutaSegura(Path path) {
        if (!path.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Ruta de documento inválida");
        }
    }

    private DocumentoDTO toDto(Documento documento) {
        DocumentoDTO dto = new DocumentoDTO();
        dto.setId(documento.getId());
        dto.setNombreArchivo(documento.getNombreArchivo());
        dto.setTipoDocumento(documento.getTipoDocumento());
        dto.setFechaSubida(documento.getFechaSubida());
        dto.setEstudianteId(documento.getEstudiante().getId());
        return dto;
    }

    public record DocumentoDescarga(Resource resource, String nombreArchivo, String contentType) {
    }
}
