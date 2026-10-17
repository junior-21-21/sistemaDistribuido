package com.example.demo.horarios.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class ParametrosHorario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Parámetro dinámico definido en la reunión de profesores
    private int maxClasesPorDiaDocente;

    public ParametrosHorario() {
        this.maxClasesPorDiaDocente = 5; 
    }

    public ParametrosHorario(int maxClasesPorDiaDocente) {
        this.maxClasesPorDiaDocente = maxClasesPorDiaDocente;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getMaxClasesPorDiaDocente() { return maxClasesPorDiaDocente; }
    public void setMaxClasesPorDiaDocente(int maxClasesPorDiaDocente) { this.maxClasesPorDiaDocente = maxClasesPorDiaDocente; }
}

