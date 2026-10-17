package com.example.demo.repository;

import com.example.demo.model.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Long> {

    List<Horario> findByDocenteId(Long docenteId);
    List<Horario> findByDocenteUsernameIgnoreCase(String username);
    List<Horario> findBySeccionId(Long seccionId);
    void deleteBySeccionId(Long seccionId);

    // Verifica si un docente ya tiene clases cruzadas en ese día y rango de horas
    @Query("SELECT h FROM Horario h WHERE h.docente.id = :docenteId AND h.diaSemana = :diaSemana AND " +
           "(h.horaInicio < :horaFin AND h.horaFin > :horaInicio)")
    List<Horario> findConflictosDocente(@Param("docenteId") Long docenteId,
                                        @Param("diaSemana") String diaSemana,
                                        @Param("horaInicio") LocalTime horaInicio,
                                        @Param("horaFin") LocalTime horaFin);

    // Verifica si un aula ya está ocupada en ese día y rango de horas
    @Query("SELECT h FROM Horario h WHERE h.aulaId = :aulaId AND h.diaSemana = :diaSemana AND " +
           "(h.horaInicio < :horaFin AND h.horaFin > :horaInicio)")
    List<Horario> findConflictosAula(@Param("aulaId") Long aulaId,
                                     @Param("diaSemana") String diaSemana,
                                     @Param("horaInicio") LocalTime horaInicio,
                                     @Param("horaFin") LocalTime horaFin);

    @Query("SELECT h FROM Horario h WHERE h.seccionId = :seccionId AND h.diaSemana = :diaSemana AND " +
           "(h.horaInicio < :horaFin AND h.horaFin > :horaInicio)")
    List<Horario> findConflictosSeccion(@Param("seccionId") Long seccionId,
                                        @Param("diaSemana") String diaSemana,
                                        @Param("horaInicio") LocalTime horaInicio,
                                        @Param("horaFin") LocalTime horaFin);

    @Query("SELECT h FROM Horario h WHERE h.docente.id = :docenteId AND h.seccionId <> :seccionId " +
           "AND h.diaSemana = :diaSemana AND (h.horaInicio < :horaFin AND h.horaFin > :horaInicio)")
    List<Horario> findConflictosDocenteExcluyendoSeccion(
            @Param("docenteId") Long docenteId,
            @Param("seccionId") Long seccionId,
            @Param("diaSemana") String diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);

    @Query("SELECT h FROM Horario h WHERE h.aulaId = :aulaId AND h.seccionId <> :seccionId " +
           "AND h.diaSemana = :diaSemana AND (h.horaInicio < :horaFin AND h.horaFin > :horaInicio)")
    List<Horario> findConflictosAulaExcluyendoSeccion(
            @Param("aulaId") Long aulaId,
            @Param("seccionId") Long seccionId,
            @Param("diaSemana") String diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);
}
