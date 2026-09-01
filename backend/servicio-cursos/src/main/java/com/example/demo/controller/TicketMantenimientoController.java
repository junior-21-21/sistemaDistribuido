package com.example.demo.controller;

import com.example.demo.model.TicketMantenimiento;
import com.example.demo.repository.TicketMantenimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketMantenimientoController {

    @Autowired
    private TicketMantenimientoRepository ticketRepository;

    @GetMapping
    public ResponseEntity<List<TicketMantenimiento>> listarTodos() {
        return ResponseEntity.ok(ticketRepository.findAll());
    }

    @GetMapping("/docente/{docenteId}")
    public ResponseEntity<List<TicketMantenimiento>> listarPorDocente(@PathVariable Long docenteId) {
        return ResponseEntity.ok(ticketRepository.findByDocenteId(docenteId));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<TicketMantenimiento>> listarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(ticketRepository.findByEstado(estado));
    }

    @PostMapping
    public ResponseEntity<TicketMantenimiento> crearTicket(@RequestBody TicketMantenimiento ticket) {
        return new ResponseEntity<>(ticketRepository.save(ticket), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<TicketMantenimiento> actualizarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        
        return ticketRepository.findById(id).map(ticket -> {
            String nuevoEstado = payload.get("estado");
            String notas = payload.get("notasResolucion");
            
            ticket.setEstado(nuevoEstado);
            if (notas != null) ticket.setNotasResolucion(notas);
            
            if ("RESUELTO".equals(nuevoEstado)) {
                ticket.setFechaResolucion(LocalDateTime.now());
            }
            
            return ResponseEntity.ok(ticketRepository.save(ticket));
        }).orElse(ResponseEntity.notFound().build());
    }
}
