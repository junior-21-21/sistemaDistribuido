package com.example.demo.repository;

import com.example.demo.model.Nota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotaRepository extends JpaRepository<Nota, Long>, JpaSpecificationExecutor<Nota> {
    List<Nota> findByEstudianteId(Long estudianteId);
    List<Nota> findByCursoId(Long cursoId);
    List<Nota> findBySeccionId(Long seccionId);
    List<Nota> findBySeccionIdAndBimestre(Long seccionId, String bimestre);
    List<Nota> findBySeccionIdAndCursoIdAndBimestreIgnoreCase(Long seccionId, Long cursoId, String bimestre);
}
