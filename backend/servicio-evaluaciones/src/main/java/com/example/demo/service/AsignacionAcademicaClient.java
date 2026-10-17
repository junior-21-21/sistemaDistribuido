package com.example.demo.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class AsignacionAcademicaClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String cursosUrl;

    public AsignacionAcademicaClient(@Value("${services.cursos-url}") String cursosUrl) {
        this.cursosUrl = cursosUrl;
    }

    public void validarPropia(String authorization, Long cursoId, Long docenteId,
                              Long seccionId, Integer anioEscolar) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) headers.set(HttpHeaders.AUTHORIZATION, authorization);
        String url = cursosUrl + "/api/asignaciones-academicas/mias/validar"
                + "?cursoId=" + cursoId + "&docenteId=" + docenteId
                + "&seccionId=" + seccionId + "&anioEscolar=" + anioEscolar;
        try {
            @SuppressWarnings("rawtypes")
            Map respuesta = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class).getBody();
            if (respuesta == null || !Boolean.TRUE.equals(respuesta.get("autorizada"))) {
                throw new AccessDeniedException("El curso y la sección no pertenecen a la carga académica del docente");
            }
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new IllegalStateException("No se pudo validar la carga académica del docente", ex);
        }
    }

    public void validarSeccionPropia(String authorization, Long seccionId, Integer anioEscolar) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) headers.set(HttpHeaders.AUTHORIZATION, authorization);
        String url = cursosUrl + "/api/asignaciones-academicas/mias/secciones/" + seccionId
                + "/validar?anioEscolar=" + anioEscolar;
        try {
            @SuppressWarnings("rawtypes")
            Map respuesta = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class).getBody();
            if (respuesta == null || !Boolean.TRUE.equals(respuesta.get("autorizada"))) {
                throw new AccessDeniedException("La sección no pertenece a la carga académica del docente");
            }
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new IllegalStateException("No se pudo validar la sección del docente", ex);
        }
    }

    public List<AsignacionScope> listarPropias(String authorization, Integer anioEscolar) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) headers.set(HttpHeaders.AUTHORIZATION, authorization);
        String url = cursosUrl + "/api/asignaciones-academicas/mias?anioEscolar=" + anioEscolar;
        try {
            AsignacionScope[] respuesta = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), AsignacionScope[].class).getBody();
            return respuesta == null ? List.of() : Arrays.asList(respuesta);
        } catch (RestClientException ex) {
            throw new IllegalStateException("No se pudo obtener la carga académica del docente", ex);
        }
    }

    public record AsignacionScope(Long cursoId, Long docenteId, Long seccionId, Integer anioEscolar) { }
}
