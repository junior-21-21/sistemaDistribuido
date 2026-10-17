package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.PonderacionCompetencia;

public interface PonderacionCompetenciaRepository extends JpaRepository<PonderacionCompetencia, Long> {
    List<PonderacionCompetencia> findBySeccionIdAndCursoIdAndBimestreIgnoreCaseAndPeriodoAnioOrderByOrdenAscIdAsc(
            Long seccionId, Long cursoId, String bimestre, String periodoAnio);
}
