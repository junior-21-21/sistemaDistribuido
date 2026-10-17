package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.IngresoColegio;

public interface IngresoColegioRepository extends JpaRepository<IngresoColegio, Long> {
    Optional<IngresoColegio> findByEstudianteIdAndFecha(Long estudianteId, LocalDate fecha);
    List<IngresoColegio> findByFechaOrderByHoraDesc(LocalDate fecha);
    List<IngresoColegio> findByEstudianteIdInAndFechaBetween(List<Long> estudianteIds, LocalDate desde, LocalDate hasta);
}
