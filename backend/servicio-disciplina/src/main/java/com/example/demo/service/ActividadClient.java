package com.example.demo.service;

import java.time.Duration;
import java.util.Map;

import com.example.demo.dto.ReporteDisciplinarioDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ActividadClient {
    private static final Logger log = LoggerFactory.getLogger(ActividadClient.class);

    private final RestTemplate restTemplate;
    private final String internalKey;
    private final boolean enabled;

    public ActividadClient(
            RestTemplateBuilder builder,
            @Value("${services.reportes-url}") String reportesUrl,
            @Value("${app.internal-api-key}") String internalKey,
            @Value("${activities.enabled:false}") boolean enabled) {
        this.restTemplate = builder
                .rootUri(reportesUrl)
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(3))
                .build();
        this.internalKey = internalKey;
        this.enabled = enabled;
    }

    public void registrarPapeleta(ReporteDisciplinarioDTO reporte) {
        if (!enabled) {
            return;
        }
        Map<String, String> actividad = Map.of(
                "texto", "Se emitió una papeleta para el estudiante ID: "
                        + reporte.getEstudianteId() + " por " + reporte.getTipo(),
                "tipo", "warning",
                "origen", "DISCIPLINA");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalKey);
        try {
            restTemplate.postForEntity(
                    "/api/internal/actividades",
                    new HttpEntity<>(actividad, headers),
                    Void.class);
        }
        catch (Exception error) {
            log.warn("No se pudo registrar la actividad del reporte {}: {}",
                    reporte.getId(), error.getMessage());
        }
    }
}
