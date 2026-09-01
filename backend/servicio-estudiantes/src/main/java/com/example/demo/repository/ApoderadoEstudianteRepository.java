package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.ApoderadoEstudiante;

@Repository
public interface ApoderadoEstudianteRepository extends JpaRepository<ApoderadoEstudiante, Long> {
}
