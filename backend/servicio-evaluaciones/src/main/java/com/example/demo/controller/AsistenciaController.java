package com.example.demo.controller;

import com.example.demo.dto.AsistenciaDTO;
import com.example.demo.dto.SesionAsistenciaDTO;
import com.example.demo.service.AsistenciaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.security.Principal;
import com.example.demo.dto.RiesgoAsistenciaDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpHeaders;
import com.example.demo.service.AsignacionAcademicaClient;
import com.example.demo.service.AsignacionAcademicaClient.AsignacionScope;
import java.time.Year;

@RestController
@RequestMapping("/api/asistencias")
@PreAuthorize("hasAnyRole('DOCENTE', 'DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
public class AsistenciaController {

    @Autowired
    private AsistenciaService asistenciaService;

    @Autowired
    private AsignacionAcademicaClient asignacionClient;

    @GetMapping
    public ResponseEntity<List<AsistenciaDTO>> listarPorSeccionYFecha(
            @RequestParam Long seccionId,
            @RequestParam LocalDate fecha,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        if (requiereAsignacionPropia(authentication)) {
            asignacionClient.validarSeccionPropia(authorization, seccionId, fecha.getYear());
        }
        return ResponseEntity.ok(asistenciaService.listarPorSeccionYFecha(seccionId, fecha));
    }

    @GetMapping("/rango")
    public ResponseEntity<List<AsistenciaDTO>> listarPorRango(
            @RequestParam Long seccionId,
            @RequestParam(required = false) Long cursoId,
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        if (requiereAsignacionPropia(authentication)) {
            asignacionClient.validarSeccionPropia(authorization, seccionId, desde.getYear());
        }
        return ResponseEntity.ok(asistenciaService.listarPorRango(seccionId, cursoId, desde, hasta));
    }

    @GetMapping("/hoy")
    public ResponseEntity<List<AsistenciaDTO>> listarHoy(
            @RequestParam Long seccionId,
            @RequestParam Long cursoId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        if (requiereAsignacionPropia(authentication)) {
            asignacionClient.validarSeccionPropia(authorization, seccionId, Year.now().getValue());
        }
        return ResponseEntity.ok(asistenciaService.listarHoy(seccionId, cursoId));
    }

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<AsistenciaDTO>> listarPorEstudiante(
            @PathVariable Long estudianteId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        return ResponseEntity.ok(filtrarPorCarga(asistenciaService.listarPorEstudiante(estudianteId),
                authorization, authentication));
    }

    @GetMapping("/horario/{horarioId}")
    public ResponseEntity<List<AsistenciaDTO>> listarPorHorario(
            @PathVariable Long horarioId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        return ResponseEntity.ok(filtrarPorCarga(asistenciaService.listarPorHorario(horarioId),
                authorization, authentication));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public ResponseEntity<AsistenciaDTO> crear(
            @Valid @RequestBody AsistenciaDTO asistenciaDTO,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Principal principal,
            Authentication authentication) {
        validarAsignacionSiCorresponde(asistenciaDTO, authorization, authentication);
        return new ResponseEntity<>(asistenciaService.guardar(asistenciaDTO, principal.getName()), HttpStatus.CREATED);
    }

    @GetMapping("/riesgo")
    public ResponseEntity<List<RiesgoAsistenciaDTO>> resumirRiesgo(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) Long seccionId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Authentication authentication) {
        if (requiereAsignacionPropia(authentication)) {
            if (seccionId == null) {
                throw new IllegalArgumentException("El docente debe seleccionar una sección asignada");
            }
            asignacionClient.validarSeccionPropia(authorization, seccionId, Year.now().getValue());
        }
        LocalDate fechaInicio = desde == null ? LocalDate.now().minusDays(14) : desde;
        return ResponseEntity.ok(asistenciaService.resumirFaltasRecientes(fechaInicio, seccionId));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public ResponseEntity<List<AsistenciaDTO>> crearMasivamente(
            @RequestBody List<@Valid AsistenciaDTO> asistenciasDTO,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Principal principal,
            Authentication authentication) {
        if (asistenciasDTO.isEmpty()) throw new IllegalArgumentException("Debe enviar al menos un registro");
        validarAsignacionSiCorresponde(asistenciasDTO.get(0), authorization, authentication);
        return new ResponseEntity<>(asistenciaService.guardarTodas(asistenciasDTO, principal.getName()), HttpStatus.CREATED);
    }

    @GetMapping("/sesiones")
    public ResponseEntity<SesionAsistenciaDTO> obtenerSesion(@RequestParam Long horarioId,
                                                              @RequestParam LocalDate fecha,
                                                              @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                              Authentication authentication) {
        SesionAsistenciaDTO sesion = asistenciaService.obtenerSesion(horarioId, fecha);
        if (sesion != null && requiereAsignacionPropia(authentication)) {
            asignacionClient.validarPropia(authorization, sesion.getCursoId(), sesion.getDocenteId(),
                    sesion.getSeccionId(), fecha.getYear());
        }
        return sesion == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(sesion);
    }

    @PostMapping("/sesiones")
    @PreAuthorize("hasAnyRole('DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public ResponseEntity<SesionAsistenciaDTO> abrirSesion(@Valid @RequestBody SesionAsistenciaDTO dto,
                                                            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                            Principal principal,
                                                            Authentication authentication) {
        if (requiereAsignacionPropia(authentication)) {
            asignacionClient.validarPropia(authorization, dto.getCursoId(), dto.getDocenteId(),
                    dto.getSeccionId(), dto.getFecha().getYear());
        }
        return new ResponseEntity<>(asistenciaService.abrirSesion(dto, principal.getName()), HttpStatus.CREATED);
    }

    @PostMapping("/sesiones/{id}/cerrar")
    @PreAuthorize("hasAnyRole('DOCENTE', 'AUXILIAR', 'RESPONSABLE_CONVIVENCIA')")
    public SesionAsistenciaDTO cerrarSesion(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            Principal principal,
            Authentication authentication) {
        SesionAsistenciaDTO sesion = asistenciaService.obtenerSesionPorId(id);
        if (requiereAsignacionPropia(authentication)) {
            asignacionClient.validarPropia(authorization, sesion.getCursoId(), sesion.getDocenteId(),
                    sesion.getSeccionId(), sesion.getFecha().getYear());
        }
        return asistenciaService.cerrarSesion(id, principal.getName());
    }

    private void validarAsignacionSiCorresponde(AsistenciaDTO dto, String authorization,
                                                 Authentication authentication) {
        if (requiereAsignacionPropia(authentication)) {
            asignacionClient.validarPropia(authorization, dto.getCursoId(), dto.getDocenteId(),
                    dto.getSeccionId(), dto.getFecha().getYear());
        }
    }

    private boolean requiereAsignacionPropia(Authentication authentication) {
        boolean docente = tieneRol(authentication, "ROLE_DOCENTE");
        boolean coberturaConvivencia = tieneRol(authentication, "ROLE_AUXILIAR")
                || tieneRol(authentication, "ROLE_RESPONSABLE_CONVIVENCIA");
        return docente && !coberturaConvivencia;
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication.getAuthorities().stream().anyMatch(a -> rol.equals(a.getAuthority()));
    }

    private List<AsistenciaDTO> filtrarPorCarga(List<AsistenciaDTO> registros, String authorization,
                                                Authentication authentication) {
        if (!requiereAsignacionPropia(authentication)) return registros;
        List<AsignacionScope> alcance = asignacionClient.listarPropias(authorization, Year.now().getValue());
        return registros.stream().filter(item -> alcance.stream().anyMatch(asignacion ->
                asignacion.cursoId().equals(item.getCursoId())
                        && asignacion.docenteId().equals(item.getDocenteId())
                        && asignacion.seccionId().equals(item.getSeccionId())
                        && asignacion.anioEscolar().equals(item.getFecha().getYear())))
                .toList();
    }
}
