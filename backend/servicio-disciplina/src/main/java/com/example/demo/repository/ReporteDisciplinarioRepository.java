package com.example.demo.repository;

import com.example.demo.model.ReporteDisciplinario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteDisciplinarioRepository extends JpaRepository<ReporteDisciplinario, Long> {
    List<ReporteDisciplinario> findByEstudianteId(Long estudianteId);
    List<ReporteDisciplinario> findBySeccionIdOrderByFechaReporteDesc(Long seccionId);
    List<ReporteDisciplinario> findAllByOrderByFechaReporteDesc();
}
