package com.example.demo.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Obtiene del servicio de cursos el alcance académico del usuario autenticado. */
@Component
public class AsignacionAcademicaClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String cursosUrl;

    public AsignacionAcademicaClient(@Value("${services.cursos-url}") String cursosUrl) {
        this.cursosUrl = cursosUrl;
    }

    public Set<Long> seccionesPropias(String authorization, int anioEscolar) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) headers.set(HttpHeaders.AUTHORIZATION, authorization);
        try {
            Asignacion[] respuesta = restTemplate.exchange(
                    cursosUrl + "/api/asignaciones-academicas/mias?anioEscolar=" + anioEscolar,
                    HttpMethod.GET, new HttpEntity<>(headers), Asignacion[].class).getBody();
            if (respuesta == null) return Set.of();
            return Arrays.stream(respuesta).map(Asignacion::seccionId).collect(Collectors.toUnmodifiableSet());
        } catch (RestClientException ex) {
            throw new IllegalStateException("No se pudo determinar la carga académica del docente", ex);
        }
    }

    private record Asignacion(Long seccionId) { }
}
