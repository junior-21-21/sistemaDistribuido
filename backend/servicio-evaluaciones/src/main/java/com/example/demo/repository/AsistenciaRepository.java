package com.example.demo.repository;

import com.example.demo.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    /** Faltas y tardanzas del rango pasan a JUSTIFICADO al aprobarse una justificación o permiso. */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
            UPDATE Asistencia a SET a.estado = 'JUSTIFICADO', a.justificacion = :motivo,
                   a.fechaModificacion = CURRENT_TIMESTAMP
            WHERE a.estudianteId = :estudianteId AND a.fecha BETWEEN :desde AND :hasta
              AND a.estado IN ('FALTA', 'TARDE')
            """)
    int justificar(@org.springframework.data.repository.query.Param("estudianteId") Long estudianteId,
                   @org.springframework.data.repository.query.Param("desde") java.time.LocalDate desde,
                   @org.springframework.data.repository.query.Param("hasta") java.time.LocalDate hasta,
                   @org.springframework.data.repository.query.Param("motivo") String motivo);
    List<Asistencia> findByEstudianteId(Long estudianteId);
    List<Asistencia> findByHorarioId(Long horarioId);
    List<Asistencia> findBySeccionIdAndFecha(Long seccionId, LocalDate fecha);
    List<Asistencia> findBySeccionIdAndCursoIdAndFecha(Long seccionId, Long cursoId, LocalDate fecha);
    List<Asistencia> findBySeccionIdAndFechaBetweenOrderByFechaAsc(Long seccionId, LocalDate desde, LocalDate hasta);
    List<Asistencia> findBySeccionIdAndCursoIdAndFechaBetweenOrderByFechaAsc(Long seccionId, Long cursoId,
                                                                              LocalDate desde, LocalDate hasta);
    List<Asistencia> findByEstadoAndFechaGreaterThanEqual(String estado, LocalDate desde);
    Optional<Asistencia> findByEstudianteIdAndHorarioIdAndFecha(Long estudianteId, Long horarioId, LocalDate fecha);
}
