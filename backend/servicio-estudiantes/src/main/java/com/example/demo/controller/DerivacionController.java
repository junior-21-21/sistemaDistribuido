package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AtenderDerivacionRequest;
import com.example.demo.dto.CrearDerivacionRequest;
import com.example.demo.dto.DerivacionDTO;
import com.example.demo.model.DerivacionPsicologica;
import com.example.demo.model.Usuario;
import com.example.demo.repository.DerivacionPsicologicaRepository;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.NotificacionClient;

import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/derivaciones")
@PreAuthorize("hasAnyRole('DOCENTE', 'DIRECTOR', 'SUBDIRECTOR', 'PSICOLOGO', 'RESPONSABLE_CONVIVENCIA')")
public class DerivacionController {

    private final DerivacionPsicologicaRepository derivacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionClient notificacionClient;

    public DerivacionController(
            DerivacionPsicologicaRepository derivacionRepository,
            EstudianteRepository estudianteRepository,
            UsuarioRepository usuarioRepository,
            NotificacionClient notificacionClient) {
        this.derivacionRepository = derivacionRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionClient = notificacionClient;
    }

    @GetMapping
    public List<DerivacionDTO> listar(Authentication authentication) {
        boolean accesoClinico = tieneAccesoClinico(authentication);
        List<DerivacionPsicologica> derivaciones = accesoClinico
                ? derivacionRepository.findAll()
                : derivacionRepository.findByDocenteDerivadorId(usuarioActual(authentication).getId());
        return derivaciones.stream()
                .map(item -> DerivacionDTO.from(item, accesoClinico))
                .toList();
    }

    @GetMapping("/estudiante/{estudianteId}")
    public List<DerivacionDTO> listarPorEstudiante(
            @PathVariable Long estudianteId,
            Authentication authentication) {
        boolean accesoClinico = tieneAccesoClinico(authentication);
        Long actorId = usuarioActual(authentication).getId();
        return derivacionRepository.findByEstudianteId(estudianteId).stream()
                .filter(item -> accesoClinico || actorId.equals(item.getDocenteDerivadorId()))
                .map(item -> DerivacionDTO.from(item, accesoClinico))
                .toList();
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyRole('DOCENTE', 'DIRECTOR', 'SUBDIRECTOR', 'RESPONSABLE_CONVIVENCIA')")
    public ResponseEntity<DerivacionDTO> crear(
            @Valid @RequestBody CrearDerivacionRequest request,
            Authentication authentication) {
        if (!estudianteRepository.existsById(request.estudianteId())) {
            throw new IllegalArgumentException("El estudiante indicado no existe");
        }
        DerivacionPsicologica derivacion = new DerivacionPsicologica();
        derivacion.setEstudianteId(request.estudianteId());
        derivacion.setDocenteDerivadorId(usuarioActual(authentication).getId());
        derivacion.setMotivo(request.motivo());
        derivacion.setDescripcion(request.descripcion().trim());
        derivacion.setPrioridad(request.prioridad());
        derivacion.setEstado("RECIBIDO");
        DerivacionPsicologica guardada = derivacionRepository.save(derivacion);
        notificacionClient.derivacion(guardada.getId(), guardada.getEstudianteId(),
                guardada.getMotivo(), guardada.getPrioridad());
        return new ResponseEntity<>(DerivacionDTO.from(guardada, tieneAccesoClinico(authentication)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/atender")
    @PreAuthorize("hasAnyRole('PSICOLOGO','DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<DerivacionDTO> atender(
            @PathVariable Long id,
            @Valid @RequestBody AtenderDerivacionRequest request) {
        return derivacionRepository.findById(id)
                .map(derivacion -> {
                    derivacion.setEstado(request.estado());
                    derivacion.setNotasPsicologo(request.notasPsicologo());
                    if ("EN_EVALUACION".equals(request.estado()) && derivacion.getFechaAtencion() == null) {
                        derivacion.setFechaAtencion(LocalDateTime.now());
                    }
                    return ResponseEntity.ok(DerivacionDTO.from(derivacionRepository.save(derivacion), true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Usuario usuarioActual(Authentication authentication) {
        return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    }

    private boolean tieneAccesoClinico(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PSICOLOGO")
                        || authority.getAuthority().equals("ROLE_DIRECTOR") || authority.getAuthority().equals("ROLE_SUBDIRECTOR"));
    }
}
