package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.AreaPendiente;

public interface AreaPendienteRepository extends JpaRepository<AreaPendiente, Long> {
    List<AreaPendiente> findByEstudianteIdOrderByAnioOrigenDesc(Long estudianteId);
    List<AreaPendiente> findByEstadoOrderByAnioOrigenDescCursoNombreAsc(String estado);
    List<AreaPendiente> findAllByOrderByAnioOrigenDescCursoNombreAsc();
    Optional<AreaPendiente> findByEstudianteIdAndCursoNombreAndAnioOrigenAndTipo(Long estudianteId, String cursoNombre,
                                                                         Integer anioOrigen, String tipo);
    List<AreaPendiente> findByEstudianteIdAndAnioOrigenAndTipo(Long estudianteId, Integer anioOrigen, String tipo);
    List<AreaPendiente> findByTipoOrderByAnioOrigenDescCursoNombreAsc(String tipo);
    List<AreaPendiente> findByTipoAndEstadoOrderByAnioOrigenDescCursoNombreAsc(String tipo, String estado);
}
