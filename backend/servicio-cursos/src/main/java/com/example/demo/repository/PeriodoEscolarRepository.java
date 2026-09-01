package com.example.demo.repository;

import com.example.demo.model.PeriodoEscolar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PeriodoEscolarRepository extends JpaRepository<PeriodoEscolar, Long> {
    List<PeriodoEscolar> findByAnioEscolarOrderByFechaInicio(Integer anioEscolar);
    Optional<PeriodoEscolar> findByAnioEscolarAndEstado(Integer anioEscolar, String estado);
    List<PeriodoEscolar> findByEstado(String estado);
}
