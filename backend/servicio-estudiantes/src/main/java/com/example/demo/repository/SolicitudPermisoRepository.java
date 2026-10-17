package com.example.demo.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.SolicitudPermiso;

public interface SolicitudPermisoRepository extends JpaRepository<SolicitudPermiso, Long> {

    @Query("""
            SELECT s FROM SolicitudPermiso s
            WHERE (:estado IS NULL OR s.estado = :estado)
              AND s.fechaHasta >= :desde AND s.fechaDesde <= :hasta
            ORDER BY CASE WHEN s.estado = 'PENDIENTE' THEN 0 ELSE 1 END, s.creadoEn DESC
            """)
    List<SolicitudPermiso> buscar(@Param("estado") String estado,
                                  @Param("desde") LocalDate desde,
                                  @Param("hasta") LocalDate hasta);

    List<SolicitudPermiso> findByEstudianteIdInOrderByCreadoEnDesc(Collection<Long> estudianteIds);

    List<SolicitudPermiso> findByTipoAndEstadoAndFechaDesdeOrderByHoraSalidaAsc(
            String tipo, String estado, LocalDate fecha);

    long countByEstado(String estado);
}
