package com.example.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.mock;

class ReconciliacionIntegridadServiceTest {

    @Test
    void detectaReferenciasHuerfanasEntreServicios() {
        WebClient estudiantes = client("""
                {"estudiantes":[{"id":1,"gradoId":1,"seccionId":99}],
                 "derivaciones":[{"id":10,"estudianteId":1,"docenteId":5}]}
                """);
        WebClient cursos = client("""
                {"gradoIds":[1],"seccionIds":[2],"cursoIds":[3],"docenteIds":[4],
                 "horarioIds":[5],"aulaIds":[6],"horarios":[],"tickets":[]}
                """);
        WebClient evaluaciones = client("""
                {"notas":[{"id":20,"estudianteId":1,"cursoId":3,"docenteId":7,"seccionId":2}],
                 "asistencias":[{"id":21,"estudianteId":1,"cursoId":3,"docenteId":4,
                 "seccionId":2,"horarioId":5}]}
                """);
        WebClient disciplina = client("""
                {"reportes":[{"id":30,"estudianteId":1,"docenteId":4,"seccionId":2}]}
                """);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ReconciliacionIntegridadService service = new ReconciliacionIntegridadService(
                estudiantes, cursos, evaluaciones, disciplina, jdbc, "clave-interna");

        var resultado = service.ejecutar();

        assertEquals(3, resultado.incidenciasDetectadas());
        verify(jdbc, atLeastOnce()).update(anyString(), any(Object[].class));
    }

    @Test
    void noReportaIncidenciasCuandoLasReferenciasSonValidas() {
        WebClient estudiantes = client("""
                {"estudiantes":[{"id":1,"gradoId":1,"seccionId":2}],"derivaciones":[]}
                """);
        WebClient cursos = client("""
                {"gradoIds":[1],"seccionIds":[2],"cursoIds":[3],"docenteIds":[4],
                 "horarioIds":[5],"aulaIds":[6],"horarios":[],"tickets":[]}
                """);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ReconciliacionIntegridadService service = new ReconciliacionIntegridadService(
                estudiantes, cursos, client("{\"notas\":[],\"asistencias\":[]}"),
                client("{\"reportes\":[]}"), jdbc, "clave-interna");

        assertEquals(0, service.ejecutar().incidenciasDetectadas());
    }

    private WebClient client(String body) {
        return WebClient.builder().exchangeFunction(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(body)
                        .build())).build();
    }
}
