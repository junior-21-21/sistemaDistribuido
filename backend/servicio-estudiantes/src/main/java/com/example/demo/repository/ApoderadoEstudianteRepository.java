package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import com.example.demo.model.ApoderadoEstudiante;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApoderadoEstudianteRepository extends JpaRepository<ApoderadoEstudiante, Long> {
    List<ApoderadoEstudiante> findByApoderadoId(Long apoderadoId);
    @EntityGraph(attributePaths = "apoderado")
    List<ApoderadoEstudiante> findByEstudianteId(Long estudianteId);
    Optional<ApoderadoEstudiante> findByApoderadoIdAndEstudianteId(Long apoderadoId, Long estudianteId);
}
