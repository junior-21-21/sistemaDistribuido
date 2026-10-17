package com.example.demo.controller;

import com.example.demo.dto.EstudianteDTO;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.MatriculaRequest;
import com.example.demo.service.EstudianteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import com.example.demo.service.AsignacionAcademicaClient;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.time.Year;
import java.security.Principal;

@RestController
@RequestMapping("/api/estudiantes")
public class EstudianteController {

    @Autowired
    private EstudianteService estudianteService;

    @Autowired
    private AsignacionAcademicaClient asignacionClient;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA', 'DOCENTE', 'PSICOLOGO', 'ENFERMERA', 'PRIMEROS_AUXILIOS', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public ResponseEntity<List<EstudianteDTO>> listarEstudiantes(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        Set<Long> alcance = alcanceDocente(authorization, authentication);
        return ResponseEntity.ok(alcance == null
                ? estudianteService.listarTodos()
                : estudianteService.listarPorSecciones(alcance));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA', 'DOCENTE', 'PSICOLOGO', 'ENFERMERA', 'PRIMEROS_AUXILIOS', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public PageResponse<EstudianteDTO> listarPaginado(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long gradoId,
            @RequestParam(required = false) Collection<Long> seccionIds,
            @RequestParam(required = false) String estado,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication,
            @PageableDefault(size = 20, sort = {"apellido", "nombre"}) Pageable pageable) {
        Set<Long> alcance = alcanceDocente(authorization, authentication);
        Collection<Long> seccionesEfectivas = alcance == null ? seccionIds : intersectar(seccionIds, alcance);
        return PageResponse.from(estudianteService.listarPaginado(q, gradoId, seccionesEfectivas, estado, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA', 'DOCENTE', 'PSICOLOGO', 'ENFERMERA', 'PRIMEROS_AUXILIOS', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public ResponseEntity<EstudianteDTO> obtenerEstudiante(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        Set<Long> alcance = alcanceDocente(authorization, authentication);
        return estudianteService.buscarPorId(id)
                .map(estudiante -> {
                    if (alcance != null && (estudiante.getSeccionId() == null
                            || !alcance.contains(estudiante.getSeccionId()))) {
                        throw new AccessDeniedException("El estudiante no pertenece a una sección asignada");
                    }
                    return estudiante;
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SECRETARIA', 'DIRECTOR')")
    public ResponseEntity<EstudianteDTO> crearEstudiante(
            @Valid @RequestBody EstudianteDTO estudianteDTO,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        EstudianteDTO nuevoEstudiante = estudianteService.guardar(estudianteDTO);
        
        try {
            Map<String, String> actividad = new HashMap<>();
            actividad.put("texto", nuevoEstudiante.getNombre() + " " + nuevoEstudiante.getApellido() + " fue matriculado");
            actividad.put("tipo", "success");
            actividad.put("origen", "ESTUDIANTES");

            if (authorization != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
                restTemplate.exchange(
                        "http://servicio-reportes:8085/api/actividades",
                        HttpMethod.POST,
                        new HttpEntity<>(actividad, headers),
                        Void.class);
            }
        } catch (Exception e) {
            System.err.println("Error al notificar actividad: " + e.getMessage());
        }

        return new ResponseEntity<>(nuevoEstudiante, HttpStatus.CREATED);
    }

    @PostMapping("/matricula")
    @PreAuthorize("hasAnyRole('SECRETARIA', 'DIRECTOR')")
    public ResponseEntity<EstudianteDTO> matricular(@Valid @RequestBody MatriculaRequest request,
            Principal principal) {
        return new ResponseEntity<>(estudianteService.matricular(request, principal.getName()), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARIA', 'DIRECTOR')")
    public ResponseEntity<EstudianteDTO> actualizarEstudiante(@PathVariable Long id, @Valid @RequestBody EstudianteDTO detalles) {
        return estudianteService.actualizar(id, detalles)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARIA', 'DIRECTOR')")
    public ResponseEntity<Void> eliminarEstudiante(@PathVariable Long id) {
        if (estudianteService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/count/total")
    public ResponseEntity<Long> countTotal() {
        return ResponseEntity.ok(estudianteService.countTotal());
    }

    @GetMapping("/count/pendientes")
    public ResponseEntity<Long> countPendientes() {
        return ResponseEntity.ok(estudianteService.countPendientes());
    }

    private Set<Long> alcanceDocente(String authorization, Authentication authentication) {
        if (!requiereAlcanceDocente(authentication)) return null;
        return asignacionClient.seccionesPropias(authorization, Year.now().getValue());
    }

    private Collection<Long> intersectar(Collection<Long> solicitadas, Set<Long> permitidas) {
        if (solicitadas == null || solicitadas.isEmpty()) return permitidas;
        return solicitadas.stream().filter(permitidas::contains).toList();
    }

    private boolean requiereAlcanceDocente(Authentication authentication) {
        if (authentication == null || !tieneRol(authentication, "ROLE_DOCENTE")) return false;
        return !((tieneRol(authentication, "ROLE_DIRECTOR") || tieneRol(authentication, "ROLE_SUBDIRECTOR"))
                || tieneRol(authentication, "ROLE_SECRETARIA")
                || tieneRol(authentication, "ROLE_PSICOLOGO")
                || tieneRol(authentication, "ROLE_ENFERMERA")
                || tieneRol(authentication, "ROLE_PRIMEROS_AUXILIOS")
                || tieneRol(authentication, "ROLE_AUXILIAR")
                || (tieneRol(authentication, "ROLE_DIRECTOR") || tieneRol(authentication, "ROLE_SUBDIRECTOR"))
                || tieneRol(authentication, "ROLE_RESPONSABLE_CONVIVENCIA"));
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication.getAuthorities().stream().anyMatch(a -> rol.equals(a.getAuthority()));
    }
}
