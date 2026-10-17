package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ConsentimientoDatosDTO;
import com.example.demo.dto.RegistrarConsentimientoRequest;
import com.example.demo.model.ConsentimientoDatos;
import com.example.demo.repository.ApoderadoEstudianteRepository;
import com.example.demo.repository.ConsentimientoDatosRepository;

@Service
public class ConsentimientoDatosService {
    private static final Set<String> FINALIDADES = Set.of(
            "DATOS_ACADEMICOS", "SALUD", "COMUNICACIONES");
    private static final Set<String> ACCIONES = Set.of("OTORGADO", "DENEGADO", "REVOCADO");
    private final ConsentimientoDatosRepository repository;
    private final ApoderadoEstudianteRepository vinculos;
    private final String versionPolitica;

    public ConsentimientoDatosService(ConsentimientoDatosRepository repository,
            ApoderadoEstudianteRepository vinculos,
            @Value("${data.consent.policy-version}") String versionPolitica) {
        this.repository = repository;
        this.vinculos = vinculos;
        this.versionPolitica = versionPolitica;
    }

    @Transactional(readOnly = true)
    public List<ConsentimientoDatosDTO> historial(Long estudianteId) {
        return repository.findByEstudianteIdOrderByRegistradoEnDesc(estudianteId).stream()
                .map(this::toDto).toList();
    }

    @Transactional
    public ConsentimientoDatosDTO registrar(RegistrarConsentimientoRequest request, String actor) {
        String finalidad = normalizar(request.finalidad());
        String accion = normalizar(request.accion());
        if (!FINALIDADES.contains(finalidad)) throw new IllegalArgumentException("Finalidad inválida");
        if (!ACCIONES.contains(accion)) throw new IllegalArgumentException("Acción de consentimiento inválida");
        vinculos.findByApoderadoIdAndEstudianteId(request.apoderadoId(), request.estudianteId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El apoderado no está vinculado al estudiante"));

        ConsentimientoDatos evento = new ConsentimientoDatos();
        evento.setEstudianteId(request.estudianteId());
        evento.setApoderadoId(request.apoderadoId());
        evento.setFinalidad(finalidad);
        evento.setAccion(accion);
        evento.setVersionPolitica(versionPolitica);
        evento.setEvidencia(limpiar(request.evidencia()));
        evento.setRegistradoPor(actor);
        evento.setRegistradoEn(LocalDateTime.now());
        return toDto(repository.save(evento));
    }

    private String normalizar(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String limpiar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ConsentimientoDatosDTO toDto(ConsentimientoDatos item) {
        return new ConsentimientoDatosDTO(item.getId(), item.getEstudianteId(), item.getApoderadoId(),
                item.getFinalidad(), item.getAccion(), item.getVersionPolitica(), item.getEvidencia(),
                item.getRegistradoPor(), item.getRegistradoEn());
    }
}
