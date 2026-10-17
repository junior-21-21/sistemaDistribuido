package com.example.demo.controller;

import com.example.demo.dto.NotaDTO;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.PonderacionesDTO;
import com.example.demo.service.PonderacionService;
import com.example.demo.service.SituacionFinalService;
import com.example.demo.service.NotaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.security.Principal;
import com.example.demo.service.AsignacionAcademicaClient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import java.time.Year;
import com.example.demo.service.AsignacionAcademicaClient.AsignacionScope;

@RestController
@RequestMapping("/api/notas")
@PreAuthorize("hasAnyRole('DOCENTE', 'DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA')")
public class NotaController {

    @Autowired
    private NotaService notaService;

    @Autowired
    private AsignacionAcademicaClient asignacionClient;

    @Autowired
    private PonderacionService ponderacionService;

    @Autowired
    private SituacionFinalService situacionFinalService;

    @GetMapping
    public ResponseEntity<List<NotaDTO>> listar(
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) String bimestre,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        return ResponseEntity.ok(notaService.listar(seccionId, bimestre,
                alcanceDocente(authorization, authentication)));
    }

    @GetMapping("/page")
    public PageResponse<NotaDTO> listarPaginado(
            @RequestParam(required = false) Long estudianteId,
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) String bimestre,
            @RequestParam(required = false) String competencia,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication,
            @PageableDefault(size = 50, sort = "estudianteId") Pageable pageable) {
        return PageResponse.from(
                notaService.listarPaginado(estudianteId, seccionId, cursoId, bimestre, competencia, pageable,
                        alcanceDocente(authorization, authentication)));
    }

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<NotaDTO>> listarPorEstudiante(
            @PathVariable Long estudianteId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        return ResponseEntity.ok(notaService.listarPorEstudiante(
                estudianteId, alcanceDocente(authorization, authentication)));
    }

    @GetMapping("/curso/{cursoId}")
    public ResponseEntity<List<NotaDTO>> listarPorCurso(
            @PathVariable Long cursoId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        return ResponseEntity.ok(notaService.listarPorCurso(
                cursoId, alcanceDocente(authorization, authentication)));
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<NotaDTO> crear(
            @Valid @RequestBody NotaDTO notaDTO,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Principal principal) {
        validarAsignacion(notaDTO, authorization);
        return new ResponseEntity<>(notaService.guardar(notaDTO), HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<List<NotaDTO>> crearMasivamente(
            @RequestBody List<@Valid NotaDTO> notasDTO,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Principal principal) {
        if (notasDTO.isEmpty()) throw new IllegalArgumentException("Debe enviar al menos una calificación");
        validarAsignacion(notasDTO.get(0), authorization);
        return new ResponseEntity<>(notaService.guardarTodas(notasDTO), HttpStatus.CREATED);
    }

    /**
     * Situación final sugerida (promovido, recuperación o permanece) de cada estudiante de la sección,
     * calculada con las reglas del MINEDU. grado = número de grado de Secundaria (1 a 5).
     */
    @GetMapping("/situacion-final")
    @PreAuthorize("hasAnyRole('SECRETARIA','DIRECTOR', 'SUBDIRECTOR')")
    public List<SituacionFinalService.SituacionEstudiante> situacionFinal(
            @RequestParam Long seccionId,
            @RequestParam int grado,
            @RequestParam(required = false) Integer anio) {
        if (grado < 1 || grado > 5) throw new IllegalArgumentException("El grado de Secundaria debe estar entre 1 y 5");
        return situacionFinalService.calcular(seccionId, anio == null ? Year.now().getValue() : anio, grado);
    }

    /** Pesos (%) de las columnas de evaluación de una sección, área y bimestre. */
    @GetMapping("/ponderaciones")
    public PonderacionesDTO listarPonderaciones(
            @RequestParam Long seccionId,
            @RequestParam Long cursoId,
            @RequestParam String bimestre,
            @RequestParam(required = false) String periodoAnio,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        if (requiereAlcanceDocente(authentication)) {
            asignacionClient.validarSeccionPropia(authorization, seccionId,
                    Integer.parseInt(PonderacionService.anio(periodoAnio)));
        }
        return ponderacionService.listar(seccionId, cursoId, bimestre, periodoAnio);
    }

    @PutMapping("/ponderaciones")
    @PreAuthorize("hasRole('DOCENTE')")
    public PonderacionesDTO guardarPonderaciones(
            @Valid @RequestBody PonderacionesDTO dto,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Principal principal) {
        asignacionClient.validarPropia(authorization, dto.getCursoId(), dto.getDocenteId(), dto.getSeccionId(),
                Integer.parseInt(PonderacionService.anio(dto.getPeriodoAnio())));
        return ponderacionService.guardar(dto, principal == null ? null : principal.getName());
    }

    /** Elimina una columna completa (sus notas y su peso) del bimestre abierto. */
    @DeleteMapping("/columna")
    @PreAuthorize("hasRole('DOCENTE')")
    public java.util.Map<String, Integer> eliminarColumna(
            @RequestParam Long seccionId,
            @RequestParam Long cursoId,
            @RequestParam Long docenteId,
            @RequestParam String bimestre,
            @RequestParam(required = false) String periodoAnio,
            @RequestParam String competencia,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        asignacionClient.validarPropia(authorization, cursoId, docenteId, seccionId,
                Integer.parseInt(PonderacionService.anio(periodoAnio)));
        int eliminadas = ponderacionService.eliminarColumna(seccionId, cursoId, bimestre, periodoAnio, competencia);
        return java.util.Map.of("notasEliminadas", eliminadas);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<NotaDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody NotaDTO notaDTO,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        validarAsignacion(notaDTO, authorization);
        return ResponseEntity.ok(notaService.actualizar(id, notaDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        validarAsignacion(notaService.buscarPorId(id), authorization);
        notaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private void validarAsignacion(NotaDTO dto, String authorization) {
        int anio;
        try {
            anio = dto.getPeriodoAnio() == null ? java.time.Year.now().getValue()
                    : Integer.parseInt(dto.getPeriodoAnio());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El año académico no es válido");
        }
        asignacionClient.validarPropia(authorization, dto.getCursoId(), dto.getDocenteId(), dto.getSeccionId(), anio);
    }

    private List<AsignacionScope> alcanceDocente(String authorization, Authentication authentication) {
        if (!requiereAlcanceDocente(authentication)) return null;
        return asignacionClient.listarPropias(authorization, Year.now().getValue());
    }

    private boolean requiereAlcanceDocente(Authentication authentication) {
        if (authentication == null || !tieneRol(authentication, "ROLE_DOCENTE")) return false;
        return !((tieneRol(authentication, "ROLE_DIRECTOR") || tieneRol(authentication, "ROLE_SUBDIRECTOR"))
                || tieneRol(authentication, "ROLE_SECRETARIA")
                || (tieneRol(authentication, "ROLE_DIRECTOR") || tieneRol(authentication, "ROLE_SUBDIRECTOR")));
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication.getAuthorities().stream().anyMatch(a -> rol.equals(a.getAuthority()));
    }
}
