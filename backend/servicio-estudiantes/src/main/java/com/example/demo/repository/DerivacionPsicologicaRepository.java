package com.example.demo.repository;

import com.example.demo.model.DerivacionPsicologica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DerivacionPsicologicaRepository extends JpaRepository<DerivacionPsicologica, Long> {
    List<DerivacionPsicologica> findByEstudianteId(Long estudianteId);
    List<DerivacionPsicologica> findByDocenteDerivadorId(Long docenteDerivadorId);
}
