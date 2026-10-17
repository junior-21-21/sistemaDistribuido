package com.example.demo.repository;

import com.example.demo.model.AsignacionAcademica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AsignacionAcademicaRepository extends JpaRepository<AsignacionAcademica, Long> {
    List<AsignacionAcademica> findBySeccionIdAndAnioEscolarAndActivaTrueOrderByCursoNombre(Long seccionId, Integer anioEscolar);
    List<AsignacionAcademica> findByAnioEscolarAndActivaTrueOrderBySeccionIdAscCursoNombreAsc(Integer anioEscolar);
    List<AsignacionAcademica> findByDocenteUsernameIgnoreCaseAndAnioEscolarAndActivaTrueOrderBySeccionIdAscCursoNombreAsc(
            String username, Integer anioEscolar);
    Optional<AsignacionAcademica> findByCursoIdAndDocenteIdAndSeccionIdAndAnioEscolarAndActivaTrue(
            Long cursoId, Long docenteId, Long seccionId, Integer anioEscolar);
    boolean existsByCursoIdAndDocenteIdAndSeccionIdAndAnioEscolarAndIdNot(
            Long cursoId, Long docenteId, Long seccionId, Integer anioEscolar, Long id);
    boolean existsByCursoIdAndDocenteIdAndSeccionIdAndAnioEscolarAndActivaTrueAndDocenteUsernameIgnoreCase(
            Long cursoId, Long docenteId, Long seccionId, Integer anioEscolar, String username);
    boolean existsBySeccionIdAndAnioEscolarAndActivaTrueAndDocenteUsernameIgnoreCase(
            Long seccionId, Integer anioEscolar, String username);

    @Query("select coalesce(sum(a.horasSemanales), 0) from AsignacionAcademica a " +
           "where a.docente.id = :docenteId and a.anioEscolar = :anio and a.activa = true " +
           "and (:excluirId is null or a.id <> :excluirId)")
    Long sumarHorasDocente(@Param("docenteId") Long docenteId,
                           @Param("anio") Integer anio,
                           @Param("excluirId") Long excluirId);
}
