package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * TicketMantenimiento — Sistema de reportes de infraestructura.
 * Permite a los docentes reportar daños en aulas (mobiliario roto, fallas eléctricas).
 */
@Entity
@Table(name = "tickets_mantenimiento", indexes = {
    @Index(name = "idx_ticket_aula", columnList = "aulaId"),
    @Index(name = "idx_ticket_estado", columnList = "estado")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long docenteId;

    @Column(nullable = false)
    private Long aulaId; // Referencia al espacio físico o sección

    @Column(nullable = false, length = 50)
    private String categoria; // MOBILIARIO, ELECTRICO, PLOMERIA, OTROS

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String urgencia; // BAJA, MEDIA, ALTA, EMERGENCIA

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE"; // PENDIENTE, EN_PROCESO, RESUELTO

    @Column(name = "fecha_reporte", nullable = false, updatable = false)
    private LocalDateTime fechaReporte;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(length = 300)
    private String notasResolucion; // Comentarios de quien reparó el daño

    @PrePersist
    protected void onCreate() {
        this.fechaReporte = LocalDateTime.now();
        if (this.estado == null) this.estado = "PENDIENTE";
    }
}
