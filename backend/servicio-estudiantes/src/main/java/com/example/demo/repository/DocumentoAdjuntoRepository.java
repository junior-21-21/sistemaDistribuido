package com.example.demo.repository;

import com.example.demo.model.DocumentoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoAdjuntoRepository extends JpaRepository<DocumentoAdjunto, Long> {
    List<DocumentoAdjunto> findByEstudianteId(Long estudianteId);
}
