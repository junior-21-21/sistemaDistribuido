package com.example.demo.service;

import com.example.demo.dto.ReporteDisciplinarioDTO;
import com.example.demo.mapper.ReporteDisciplinarioMapper;
import com.example.demo.model.ReporteDisciplinario;
import com.example.demo.repository.ReporteDisciplinarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteDisciplinarioServiceTest {

    @Mock
    private ReporteDisciplinarioRepository repository;

    @Mock
    private ReporteDisciplinarioMapper mapper;

    @InjectMocks
    private ReporteDisciplinarioService service;

    @Test
    void marcarNotificadoRegistraFechaYBandera() {
        ReporteDisciplinario entity = new ReporteDisciplinario();
        ReporteDisciplinarioDTO dto = new ReporteDisciplinarioDTO();
        dto.setEstado("NOTIFICADO");

        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        service.actualizarEstado(7L, "NOTIFICADO");

        assertEquals("NOTIFICADO", entity.getEstado());
        assertTrue(entity.getApoderadoNotificado());
        assertNotNull(entity.getFechaNotificacion());
    }

    @Test
    void rechazaEstadoDesconocido() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.actualizarEstado(7L, "BORRADO"));

        assertEquals("Estado disciplinario no válido", error.getMessage());
    }
}
