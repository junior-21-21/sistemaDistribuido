package com.example.demo.repository;

import com.example.demo.model.Grado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GradoRepository extends JpaRepository<Grado, Long> {
    List<Grado> findByNivelIdAndActivoTrue(Long nivelId);
    List<Grado> findByActivoTrue();
}
