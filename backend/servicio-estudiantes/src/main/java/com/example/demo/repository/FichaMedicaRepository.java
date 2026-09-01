package com.example.demo.repository;

import com.example.demo.model.FichaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FichaMedicaRepository extends JpaRepository<FichaMedica, Long> {
    Optional<FichaMedica> findByEstudianteId(Long estudianteId);
    boolean existsByEstudianteId(Long estudianteId);
}
