package com.example.demo.repository;

import java.util.Optional;

import com.example.demo.model.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Long> {
    long countByEstado(String estado);
    Optional<Docente> findByEmailIgnoreCase(String email);
    Optional<Docente> findByUsernameIgnoreCase(String username);
}
