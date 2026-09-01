package com.example.demo.repository;

import com.example.demo.model.Nivel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NivelRepository extends JpaRepository<Nivel, Long> {
    List<Nivel> findByActivoTrue();
}
