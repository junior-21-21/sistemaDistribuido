package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.NominaMatricula;

public interface NominaMatriculaRepository extends JpaRepository<NominaMatricula, Long> {
    List<NominaMatricula> findByAnioEscolar(Integer anioEscolar);
    Optional<NominaMatricula> findBySeccionIdAndAnioEscolar(Long seccionId, Integer anioEscolar);
}
