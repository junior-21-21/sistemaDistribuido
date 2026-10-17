package com.example.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.BimestreEstado;

public interface BimestreEstadoRepository extends JpaRepository<BimestreEstado, Long> {
    Optional<BimestreEstado> findByAnioAndBimestre(Integer anio, String bimestre);
}
