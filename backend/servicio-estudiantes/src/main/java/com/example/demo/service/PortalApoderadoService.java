package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.example.demo.dto.DocumentoDTO;
import com.example.demo.dto.PortalEstudianteDTO;
import com.example.demo.dto.PortalResumenDTO;
import com.example.demo.model.ApoderadoEstudiante;
import com.example.demo.model.Usuario;
import com.example.demo.repository.ApoderadoEstudianteRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class PortalApoderadoService {

    private final UsuarioRepository usuarioRepository;
    private final ApoderadoEstudianteRepository vinculoRepository;
    private final DocumentoService documentoService;
    private final RestClient evaluacionesClient;
    private final String internalApiKey;

    public PortalApoderadoService(
            UsuarioRepository usuarioRepository,
            ApoderadoEstudianteRepository vinculoRepository,
            DocumentoService documentoService,
            RestClient.Builder restClientBuilder,
            @Value("${services.evaluaciones-url:http://servicio-evaluaciones:8084}") String evaluacionesUrl,
            @Value("${app.internal-api-key}") String internalApiKey) {
        this.usuarioRepository = usuarioRepository;
        this.vinculoRepository = vinculoRepository;
        this.documentoService = documentoService;
        this.evaluacionesClient = restClientBuilder.baseUrl(evaluacionesUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public List<PortalEstudianteDTO> listarEstudiantes(String username) {
        Usuario usuario = usuarioApoderado(username);
        return vinculoRepository.findByApoderadoId(usuario.getApoderado().getId()).stream()
                .map(this::toPortalEstudiante)
                .toList();
    }

    public PortalResumenDTO obtenerResumen(String username, Long estudianteId) {
        ApoderadoEstudiante vinculo = validarVinculo(username, estudianteId);
        List<DocumentoDTO> documentos = documentoService.listarDocumentosPorEstudiante(estudianteId);
        List<Map<String, Object>> notas = obtenerLista("/api/internal/portal/estudiantes/{id}/notas", estudianteId);
        List<Map<String, Object>> asistencias = obtenerLista(
                "/api/internal/portal/estudiantes/{id}/asistencias", estudianteId);
        return new PortalResumenDTO(toPortalEstudiante(vinculo), documentos, notas, asistencias);
    }

    public DocumentoService.DocumentoDescarga descargar(
            String username, Long estudianteId, Long documentoId) throws java.io.IOException {
        validarVinculo(username, estudianteId);
        return documentoService.cargarArchivoComoRecurso(estudianteId, documentoId);
    }

    private List<Map<String, Object>> obtenerLista(String path, Long estudianteId) {
        return evaluacionesClient.get()
                .uri(path, estudianteId)
                .header("X-Internal-Api-Key", internalApiKey)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    private ApoderadoEstudiante validarVinculo(String username, Long estudianteId) {
        Usuario usuario = usuarioApoderado(username);
        return vinculoRepository.findByApoderadoIdAndEstudianteId(usuario.getApoderado().getId(), estudianteId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "El estudiante no está asociado a este apoderado"));
    }

    private Usuario usuarioApoderado(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!usuario.getRoles().contains("APODERADO") || usuario.getApoderado() == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "La cuenta no está vinculada a un apoderado");
        }
        return usuario;
    }

    private PortalEstudianteDTO toPortalEstudiante(ApoderadoEstudiante vinculo) {
        var estudiante = vinculo.getEstudiante();
        return new PortalEstudianteDTO(estudiante.getId(), estudiante.getNombre(), estudiante.getApellido(),
                estudiante.getGradoId(), estudiante.getSeccionId(), vinculo.getParentesco(),
                Boolean.TRUE.equals(vinculo.getPermisoRetiro()));
    }
}
