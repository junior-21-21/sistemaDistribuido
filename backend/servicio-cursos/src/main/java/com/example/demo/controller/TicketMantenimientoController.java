package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ActualizarTicketRequest;
import com.example.demo.dto.CrearTicketRequest;
import com.example.demo.model.Docente;
import com.example.demo.model.TicketMantenimiento;
import com.example.demo.repository.AulaRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.TicketMantenimientoRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tickets")
@PreAuthorize("hasAnyRole('DOCENTE','MANTENIMIENTO','DIRECTOR', 'SUBDIRECTOR')")
public class TicketMantenimientoController {

    private final TicketMantenimientoRepository ticketRepository;
    private final DocenteRepository docenteRepository;
    private final AulaRepository aulaRepository;

    public TicketMantenimientoController(
            TicketMantenimientoRepository ticketRepository,
            DocenteRepository docenteRepository,
            AulaRepository aulaRepository) {
        this.ticketRepository = ticketRepository;
        this.docenteRepository = docenteRepository;
        this.aulaRepository = aulaRepository;
    }

    @GetMapping
    public List<TicketMantenimiento> listar(@AuthenticationPrincipal Jwt jwt) {
        if (tieneRol(jwt, "MANTENIMIENTO") || (tieneRol(jwt, "DIRECTOR") || tieneRol(jwt, "SUBDIRECTOR"))) {
            return ticketRepository.findAll();
        }
        return ticketRepository.findByDocenteId(docenteActual(jwt).getId());
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<TicketMantenimiento> crear(
            @Valid @RequestBody CrearTicketRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        aulaRepository.findById(request.aulaId())
                .filter(aula -> Boolean.TRUE.equals(aula.getActiva()))
                .orElseThrow(() -> new IllegalArgumentException("El aula indicada no existe o está inactiva"));

        TicketMantenimiento ticket = new TicketMantenimiento();
        ticket.setDocenteId(docenteActual(jwt).getId());
        ticket.setAulaId(request.aulaId());
        ticket.setCategoria(request.categoria());
        ticket.setDescripcion(request.descripcion().trim());
        ticket.setUrgencia(request.urgencia());
        ticket.setEstado("PENDIENTE");
        return new ResponseEntity<>(ticketRepository.save(ticket), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('MANTENIMIENTO','DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<TicketMantenimiento> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarTicketRequest request) {
        return ticketRepository.findById(id).map(ticket -> {
            if ("RESUELTO".equals(request.estado())
                    && (request.notasResolucion() == null || request.notasResolucion().isBlank())) {
                throw new IllegalArgumentException("Las notas de resolución son obligatorias al resolver un ticket");
            }
            ticket.setEstado(request.estado());
            ticket.setNotasResolucion(request.notasResolucion());
            ticket.setFechaResolucion("RESUELTO".equals(request.estado()) ? LocalDateTime.now() : null);
            return ResponseEntity.ok(ticketRepository.save(ticket));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Docente docenteActual(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El token no contiene el correo del docente");
        }
        return docenteRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La cuenta autenticada no está vinculada a un perfil docente"));
    }

    private boolean tieneRol(Jwt jwt, String rol) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && roles.contains(rol)) {
            return true;
        }
        return rol.equals(jwt.getClaimAsString("rol"));
    }
}
