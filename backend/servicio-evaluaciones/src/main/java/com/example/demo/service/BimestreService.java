package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.BimestreAuditoriaDTO;
import com.example.demo.dto.BimestreEstadoDTO;
import com.example.demo.exception.ConflictoAcademicoException;
import com.example.demo.model.BimestreAuditoria;
import com.example.demo.model.BimestreEstado;
import com.example.demo.repository.BimestreAuditoriaRepository;
import com.example.demo.repository.BimestreEstadoRepository;

@Service
public class BimestreService {
    private static final List<String> BIMESTRES = List.of(
            "I BIMESTRE", "II BIMESTRE", "III BIMESTRE", "IV BIMESTRE");
    private final BimestreEstadoRepository estados;
    private final BimestreAuditoriaRepository auditoria;

    public BimestreService(BimestreEstadoRepository estados, BimestreAuditoriaRepository auditoria) {
        this.estados = estados;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<BimestreEstadoDTO> listar(int anio) {
        return BIMESTRES.stream().map(bimestre -> estados.findByAnioAndBimestre(anio, bimestre)
                .map(this::toDto)
                .orElse(new BimestreEstadoDTO(anio, bimestre, "ABIERTO", null, null))).toList();
    }

    @Transactional(readOnly = true)
    public List<BimestreAuditoriaDTO> auditoria(int anio, String bimestre) {
        return auditoria.findByAnioAndBimestreOrderByOcurridoEnDesc(anio, normalizar(bimestre)).stream()
                .map(item -> new BimestreAuditoriaDTO(item.getAccion(), item.getMotivo(),
                        item.getActor(), item.getOcurridoEn())).toList();
    }

    @Transactional
    public BimestreEstadoDTO cerrar(int anio, String bimestre, String motivo, String actor) {
        validarAnio(anio);
        String periodo = normalizar(bimestre);
        BimestreEstado estado = estados.findByAnioAndBimestre(anio, periodo).orElseGet(BimestreEstado::new);
        if ("CERRADO".equals(estado.getEstado())) {
            throw new ConflictoAcademicoException("El bimestre ya se encuentra cerrado");
        }
        estado.setAnio(anio);
        estado.setBimestre(periodo);
        estado.setEstado("CERRADO");
        estado.setActualizadoPor(actor);
        estado.setActualizadoEn(LocalDateTime.now());
        registrar(anio, periodo, "CIERRE", motivo, actor);
        return toDto(estados.save(estado));
    }

    @Transactional
    public BimestreEstadoDTO reabrir(int anio, String bimestre, String motivo, String actor) {
        String periodo = normalizar(bimestre);
        BimestreEstado estado = estados.findByAnioAndBimestre(anio, periodo)
                .orElseThrow(() -> new ConflictoAcademicoException("El bimestre no está cerrado"));
        if (!"CERRADO".equals(estado.getEstado())) {
            throw new ConflictoAcademicoException("El bimestre ya se encuentra abierto");
        }
        estado.setEstado("ABIERTO");
        estado.setActualizadoPor(actor);
        estado.setActualizadoEn(LocalDateTime.now());
        registrar(anio, periodo, "REAPERTURA", motivo, actor);
        return toDto(estados.save(estado));
    }

    @Transactional(readOnly = true)
    public void validarAbierto(int anio, String bimestre) {
        String periodo = normalizar(bimestre);
        if (estados.findByAnioAndBimestre(anio, periodo)
                .map(item -> "CERRADO".equals(item.getEstado())).orElse(false)) {
            throw new ConflictoAcademicoException(
                    periodo + " de " + anio + " está cerrado; solicite una reapertura auditada");
        }
    }

    private void registrar(int anio, String bimestre, String accion, String motivo, String actor) {
        BimestreAuditoria evento = new BimestreAuditoria();
        evento.setAnio(anio);
        evento.setBimestre(bimestre);
        evento.setAccion(accion);
        evento.setMotivo(motivo.trim());
        evento.setActor(actor);
        evento.setOcurridoEn(LocalDateTime.now());
        auditoria.save(evento);
    }

    private String normalizar(String value) {
        String normalizado = value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        normalizado = switch (normalizado) {
            case "I", "1" -> "I BIMESTRE";
            case "II", "2" -> "II BIMESTRE";
            case "III", "3" -> "III BIMESTRE";
            case "IV", "4" -> "IV BIMESTRE";
            default -> normalizado;
        };
        if (!Set.copyOf(BIMESTRES).contains(normalizado)) {
            throw new IllegalArgumentException("Bimestre inválido");
        }
        return normalizado;
    }

    private void validarAnio(int anio) {
        if (anio < 2000 || anio > 2100) throw new IllegalArgumentException("Año académico inválido");
    }

    private BimestreEstadoDTO toDto(BimestreEstado item) {
        return new BimestreEstadoDTO(item.getAnio(), item.getBimestre(), item.getEstado(),
                item.getActualizadoPor(), item.getActualizadoEn());
    }
}
