package com.example.notificaciones.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.notificaciones.model.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    Optional<Notificacion> findByEventoIdAndCanal(String eventoId, String canal);

    @Query("select n from Notificacion n where " +
           "(:estado = '' or n.estado = :estado) and (:canal = '' or n.canal = :canal) and " +
           "(:tipo = '' or n.tipo = :tipo) and (:estudianteId is null or n.estudianteId = :estudianteId)")
    Page<Notificacion> buscar(@Param("estado") String estado, @Param("canal") String canal,
                              @Param("tipo") String tipo, @Param("estudianteId") Long estudianteId,
                              Pageable pageable);

    @Query("select n from Notificacion n where n.estado = 'REINTENTO' and n.proximoIntento <= :ahora")
    List<Notificacion> buscarReintentos(@Param("ahora") LocalDateTime ahora);
}
