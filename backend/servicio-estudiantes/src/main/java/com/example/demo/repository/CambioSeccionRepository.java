package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.CambioSeccion;

public interface CambioSeccionRepository extends JpaRepository<CambioSeccion, Long> {
    List<CambioSeccion> findByEstudianteIdOrderByFechaDesc(Long estudianteId);
}
