package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.model.AtencionMedica;

public interface AtencionMedicaRepository extends JpaRepository<AtencionMedica, Long> {
    List<AtencionMedica> findByFichaMedicaIdOrderByFechaAtencionDesc(Long fichaMedicaId);
    Page<AtencionMedica> findByFichaMedicaId(Long fichaMedicaId, Pageable pageable);
}
