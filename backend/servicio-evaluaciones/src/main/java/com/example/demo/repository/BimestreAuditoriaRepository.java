package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.BimestreAuditoria;

public interface BimestreAuditoriaRepository extends JpaRepository<BimestreAuditoria, Long> {
    List<BimestreAuditoria> findByAnioAndBimestreOrderByOcurridoEnDesc(Integer anio, String bimestre);
}
