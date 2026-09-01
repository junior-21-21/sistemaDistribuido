package com.example.demo.repository;

import com.example.demo.model.Aula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    List<Aula> findByActivaTrue();
    List<Aula> findByTurnoDisponibleInAndActivaTrue(List<String> turnos);
}
