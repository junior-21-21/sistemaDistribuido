package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** Módulo opcional (Psicología, Enfermería, Mantenimiento) que Dirección habilita si tiene ese personal. */
@Entity
@Table(name = "modulos_institucion")
@Data
public class ModuloInstitucion {

    @Id
    @Column(length = 30)
    private String codigo;

    @Column(nullable = false)
    private boolean habilitado;

    @Column(name = "actualizado_por", nullable = false, length = 100)
    private String actualizadoPor;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
