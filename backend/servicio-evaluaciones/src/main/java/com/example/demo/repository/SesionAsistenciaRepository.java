package com.example.demo.repository;

import com.example.demo.model.SesionAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SesionAsistenciaRepository extends JpaRepository<SesionAsistencia, Long> {
    Optional<SesionAsistencia> findByHorarioIdAndFecha(Long horarioId, LocalDate fecha);
}
