package com.example.demo.service;

import com.example.demo.dto.NotaDTO;
import com.example.demo.mapper.NotaMapper;
import com.example.demo.model.Nota;
import com.example.demo.repository.NotaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotaServiceTest {

    @Mock
    private NotaRepository repository;

    @Mock
    private NotaMapper mapper;

    @Mock
    private BimestreService bimestreService;

    @InjectMocks
    private NotaService service;

    @Test
    void guardaNotasMasivasEnUnaSolaOperacion() {
        NotaDTO dto = new NotaDTO();
        dto.setSeccionId(1L);
        dto.setCursoId(2L);
        dto.setBimestre("I BIMESTRE");
        dto.setEstudianteId(3L);
        dto.setCompetencia("Lee textos");
        Nota entity = new Nota();

        when(repository.findBySeccionIdAndCursoIdAndBimestreIgnoreCase(1L, 2L, "I BIMESTRE"))
                .thenReturn(List.of());
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.saveAll(anyList())).thenReturn(List.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        List<NotaDTO> resultado = service.guardarTodas(List.of(dto));

        assertEquals(1, resultado.size());
        verify(repository).saveAll(List.of(entity));
    }

    @Test
    void rechazaActualizacionDeNotaInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.actualizar(99L, new NotaDTO()));

        assertEquals("Nota no encontrada", error.getMessage());
    }

    @Test
    void actualizaLaCalificacionExistenteSinDuplicarla() {
        NotaDTO dto = new NotaDTO();
        dto.setSeccionId(1L);
        dto.setCursoId(2L);
        dto.setBimestre("I BIMESTRE");
        dto.setEstudianteId(3L);
        dto.setCompetencia("Lee textos");

        Nota existente = new Nota();
        existente.setId(50L);
        existente.setSeccionId(1L);
        existente.setCursoId(2L);
        existente.setBimestre("I BIMESTRE");
        existente.setEstudianteId(3L);
        existente.setCompetencia("Lee textos");

        when(repository.findBySeccionIdAndCursoIdAndBimestreIgnoreCase(1L, 2L, "I BIMESTRE"))
                .thenReturn(List.of(existente));
        when(repository.saveAll(List.of(existente))).thenReturn(List.of(existente));
        when(mapper.toDto(existente)).thenReturn(dto);

        service.guardarTodas(List.of(dto));

        verify(mapper).updateEntity(dto, existente);
        verify(repository).saveAll(List.of(existente));
    }
}
