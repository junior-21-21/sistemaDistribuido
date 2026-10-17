package com.example.demo.controller;

import com.example.demo.dto.HorarioDTO;
import com.example.demo.service.HorarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    @Autowired
    private HorarioService horarioService;

    @GetMapping("/seccion/{seccionId}")
    public ResponseEntity<List<HorarioDTO>> listarPorSeccion(@PathVariable Long seccionId) {
        return ResponseEntity.ok(horarioService.listarPorSeccion(seccionId));
    }

    @GetMapping("/docente/{docenteId}")
    public ResponseEntity<List<HorarioDTO>> listarPorDocente(@PathVariable Long docenteId) {
        return ResponseEntity.ok(horarioService.listarPorDocente(docenteId));
    }

    @GetMapping("/mios")
    public ResponseEntity<List<HorarioDTO>> listarMios(Principal principal) {
        return ResponseEntity.ok(horarioService.listarMios(principal.getName()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<?> crear(@Valid @RequestBody HorarioDTO horarioDTO) {
        try {
            return new ResponseEntity<>(horarioService.guardar(horarioDTO), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/seccion/{seccionId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<?> reemplazarPorSeccion(
            @PathVariable Long seccionId,
            @Valid @RequestBody List<@Valid HorarioDTO> horarios) {
        try {
            return ResponseEntity.ok(horarioService.reemplazarPorSeccion(seccionId, horarios));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Guarda varias secciones juntas (por ejemplo, un turno completo recién generado). */
    @PutMapping("/lote")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<?> reemplazarVariasSecciones(
            @Valid @RequestBody List<@Valid SeccionHorarios> secciones) {
        try {
            java.util.Map<Long, List<HorarioDTO>> porSeccion = new java.util.LinkedHashMap<>();
            for (SeccionHorarios item : secciones) {
                porSeccion.put(item.seccionId(), item.bloques() == null ? List.of() : item.bloques());
            }
            return ResponseEntity.ok(horarioService.reemplazarVariasSecciones(porSeccion));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public record SeccionHorarios(@jakarta.validation.constraints.NotNull Long seccionId,
                                  List<@Valid HorarioDTO> bloques) {
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (horarioService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
