package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.example.demo.model.Estudiante;
import com.example.demo.repository.DocumentoRepository;
import com.example.demo.repository.EstudianteRepository;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock private DocumentoRepository documentoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @TempDir Path tempDir;

    @Test
    void rechazaUnPdfConFirmaBinariaFalsa() {
        DocumentoService service = new DocumentoService(
                documentoRepository, estudianteRepository, tempDir.toString());
        MockMultipartFile archivo = new MockMultipartFile(
                "file", "documento.pdf", "application/pdf", "contenido falso".getBytes());
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(new Estudiante()));

        assertThrows(IllegalArgumentException.class,
                () -> service.guardarDocumento(1L, "DNI", archivo));

        verify(documentoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
