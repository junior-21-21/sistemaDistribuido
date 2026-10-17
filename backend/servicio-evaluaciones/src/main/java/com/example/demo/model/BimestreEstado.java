package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Table(name = "bimestres_estado", uniqueConstraints =
        @UniqueConstraint(name = "uk_bimestre_anio", columnNames = {"anio", "bimestre"}))
@Data
public class BimestreEstado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Integer anio;
    @Column(nullable = false, length = 20) private String bimestre;
    @Column(nullable = false, length = 12) private String estado = "ABIERTO";
    @Column(name = "actualizado_por", nullable = false, length = 100) private String actualizadoPor;
    @Column(name = "actualizado_en", nullable = false) private LocalDateTime actualizadoEn;
    @Version private Long version;
}
