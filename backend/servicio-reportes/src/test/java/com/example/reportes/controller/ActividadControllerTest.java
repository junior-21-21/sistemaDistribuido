package com.example.reportes.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.example.reportes.model.Actividad;
import com.example.reportes.repository.ActividadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActividadControllerTest {
    @Mock ActividadRepository repository;

    @Test
    void asignaFechaAlRegistrarUnaActividad() {
        Actividad actividad = new Actividad(null, "Horario publicado", "HORARIO", null, "CURSOS");
        when(repository.save(any(Actividad.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new ActividadController(repository).registrarActividad(actividad);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody().getFecha());
        verify(repository).save(actividad);
    }

    @Test
    void devuelveActividadesRecientesOrdenadasPorRepositorio() {
        Actividad actividad = new Actividad(1L, "Nota registrada", "NOTA",
                LocalDateTime.now(), "EVALUACIONES");
        when(repository.findTop10ByOrderByFechaDesc()).thenReturn(List.of(actividad));

        var response = new ActividadController(repository).obtenerRecientes();

        assertEquals(List.of(actividad), response.getBody());
    }
}
