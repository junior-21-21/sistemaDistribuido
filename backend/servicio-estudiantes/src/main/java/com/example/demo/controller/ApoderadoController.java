package com.example.demo.controller;

import com.example.demo.dto.ApoderadoDTO;
import com.example.demo.service.ApoderadoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apoderados")
@PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'SECRETARIA')")
public class ApoderadoController {

    @Autowired
    private ApoderadoService apoderadoService;

    @GetMapping
    public ResponseEntity<List<ApoderadoDTO>> listarApoderados() {
        return ResponseEntity.ok(apoderadoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApoderadoDTO> obtenerApoderado(@PathVariable Long id) {
        return apoderadoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<ApoderadoDTO> obtenerPorDni(@PathVariable String dni) {
        return apoderadoService.buscarPorDni(dni)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SECRETARIA')")
    public ResponseEntity<ApoderadoDTO> crearApoderado(@Valid @RequestBody ApoderadoDTO apoderadoDTO) {
        ApoderadoDTO nuevoApoderado = apoderadoService.guardar(apoderadoDTO);
        return new ResponseEntity<>(nuevoApoderado, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARIA')")
    public ResponseEntity<ApoderadoDTO> actualizarApoderado(@PathVariable Long id, @Valid @RequestBody ApoderadoDTO detalles) {
        return apoderadoService.actualizar(id, detalles)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARIA')")
    public ResponseEntity<Void> eliminarApoderado(@PathVariable Long id) {
        if (apoderadoService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
