package com.example.demo.horarios.repository;

import com.example.demo.horarios.domain.ParametrosHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametrosHorarioRepository extends JpaRepository<ParametrosHorario, Long> {
}
