package com.example.demo.repository;

import com.example.demo.model.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeccionRepository extends JpaRepository<Seccion, Long> {
    List<Seccion> findByGradoIdAndActivaTrue(Long gradoId);
    List<Seccion> findByAnioEscolarAndActivaTrue(Integer anioEscolar);
    List<Seccion> findByTurnoAndAnioEscolarAndActivaTrue(String turno, Integer anioEscolar);
    long countByGradoIdAndActivaTrue(Long gradoId);

    /** Verifica si ya existe una sección con el mismo nombre, grado, turno y año (excluyendo el id actual en edición) */
    boolean existsByNombreIgnoreCaseAndGradoIdAndTurnoAndAnioEscolarAndIdNot(
        String nombre, Long gradoId, String turno, Integer anioEscolar, Long id);

    boolean existsByNombreIgnoreCaseAndGradoIdAndTurnoAndAnioEscolar(
        String nombre, Long gradoId, String turno, Integer anioEscolar);
}
