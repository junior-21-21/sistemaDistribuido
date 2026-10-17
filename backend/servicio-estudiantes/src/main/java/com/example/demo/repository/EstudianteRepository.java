package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.demo.model.Estudiante;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long>, JpaSpecificationExecutor<Estudiante> {
    boolean existsByDni(String dni);
    boolean existsByEmail(String email);
    Optional<Estudiante> findByDni(String dni);
    Optional<Estudiante> findByEmailIgnoreCase(String email);
    long countByEstado(String estado);
    java.util.List<Estudiante> findBySeccionIdAndEstadoOrderByApellidoAscNombreAsc(Long seccionId, String estado);

    @org.springframework.data.jpa.repository.Query(
            "SELECT e.seccionId, COUNT(e) FROM Estudiante e WHERE e.estado = 'ACTIVO' AND e.seccionId IS NOT NULL GROUP BY e.seccionId")
    java.util.List<Object[]> contarActivosPorSeccion();
}
