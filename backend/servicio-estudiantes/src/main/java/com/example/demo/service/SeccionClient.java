package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Consulta al servicio de cursos los datos de una sección (grado, año, capacidad). */
@Component
public class SeccionClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String cursosUrl;

    public SeccionClient(@Value("${services.cursos-url}") String cursosUrl) {
        this.cursosUrl = cursosUrl;
    }

    public record Seccion(Long id, String nombre, Long gradoId, String gradoNombre, Integer capacidadMaxima,
                          Boolean activa, Integer anioEscolar) {
    }

    public Seccion obtener(Long seccionId, String authorization) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) headers.set(HttpHeaders.AUTHORIZATION, authorization);
        try {
            return restTemplate.exchange(cursosUrl + "/api/secciones/" + seccionId, HttpMethod.GET,
                    new HttpEntity<>(headers), Seccion.class).getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalArgumentException("La sección de destino no existe");
        } catch (RestClientException ex) {
            throw new IllegalStateException("No se pudo consultar la sección de destino", ex);
        }
    }
}
