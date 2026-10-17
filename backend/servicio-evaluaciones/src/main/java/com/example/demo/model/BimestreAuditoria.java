package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "bimestres_auditoria")
@Data
public class BimestreAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Integer anio;
    @Column(nullable = false, length = 20) private String bimestre;
    @Column(nullable = false, length = 20) private String accion;
    @Column(nullable = false, length = 500) private String motivo;
    @Column(nullable = false, length = 100) private String actor;
    @Column(name = "ocurrido_en", nullable = false) private LocalDateTime ocurridoEn;
}
