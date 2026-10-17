package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.ConsentimientoDatos;

public interface ConsentimientoDatosRepository extends JpaRepository<ConsentimientoDatos, Long> {
    List<ConsentimientoDatos> findByEstudianteIdOrderByRegistradoEnDesc(Long estudianteId);
}
