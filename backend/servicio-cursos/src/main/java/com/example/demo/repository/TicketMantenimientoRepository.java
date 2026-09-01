package com.example.demo.repository;

import com.example.demo.model.TicketMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketMantenimientoRepository extends JpaRepository<TicketMantenimiento, Long> {
    List<TicketMantenimiento> findByEstado(String estado);
    List<TicketMantenimiento> findByDocenteId(Long docenteId);
    List<TicketMantenimiento> findByAulaId(Long aulaId);
}
