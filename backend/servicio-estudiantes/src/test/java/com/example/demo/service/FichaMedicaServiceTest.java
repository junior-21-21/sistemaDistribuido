package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dto.FichaMedicaRequest;
import com.example.demo.model.FichaMedica;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.repository.FichaMedicaRepository;

@ExtendWith(MockitoExtension.class)
class FichaMedicaServiceTest {

    @Mock private FichaMedicaRepository repository;
    @Mock private EstudianteRepository estudianteRepository;

    @Test
    void registraElUsuarioAutenticadoComoAuditor() {
        FichaMedicaRequest request = request(10L);
        when(estudianteRepository.existsById(10L)).thenReturn(true);
        when(repository.existsByEstudianteId(10L)).thenReturn(false);
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        FichaMedica resultado = new FichaMedicaService(repository, estudianteRepository)
                .crear(request, "enfermera.autenticada");

        assertEquals("enfermera.autenticada", resultado.getUltimaActualizacionPor());
        verify(repository).save(resultado);
    }

    @Test
    void rechazaFichaParaEstudianteInexistente() {
        FichaMedicaRequest request = request(99L);
        when(estudianteRepository.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> new FichaMedicaService(repository, estudianteRepository).crear(request, "enfermera"));

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private FichaMedicaRequest request(Long estudianteId) {
        return new FichaMedicaRequest(
                estudianteId, "O+", null, null, null, null, null,
                "SIS", null, "Contacto", "999888777", "Madre");
    }
}
