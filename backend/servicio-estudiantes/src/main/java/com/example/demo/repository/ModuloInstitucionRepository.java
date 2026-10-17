package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.ModuloInstitucion;

public interface ModuloInstitucionRepository extends JpaRepository<ModuloInstitucion, String> {
}
