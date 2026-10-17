package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Matricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    Optional<Matricula> findByEstudianteIdAndAnioEscolar(Long estudianteId, Integer anioEscolar);
    List<Matricula> findByEstudianteIdOrderByAnioEscolarDesc(Long estudianteId);
}
