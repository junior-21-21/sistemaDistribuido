package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.repository.ApoderadoRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository repository;
    @Mock private ApoderadoRepository apoderadoRepository;
    @Mock private PasswordEncoder encoder;
    @Mock private ModuloInstitucionService modulos;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repository, apoderadoRepository, encoder, modulos);
    }

    @Test
    void impideDesactivarLaCuentaDelUsuarioActual() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setUsername("director");
        when(repository.findById(7L)).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class,
                () -> service.cambiarEstado(7L, "PAUSADO", "director"));

        verify(repository, never()).save(usuario);
    }

    @Test
    void generaUnaClaveTemporalFuerteYLaCodifica() {
        Usuario usuario = new Usuario();
        usuario.setId(8L);
        usuario.setUsername("docente");
        when(repository.findById(8L)).thenReturn(Optional.of(usuario));
        when(encoder.encode(org.mockito.ArgumentMatchers.anyString())).thenReturn("hash");

        String clave = service.restablecerClave(8L);

        assertTrue(clave.length() >= 14);
        assertTrue(clave.matches(".*[A-Z].*"));
        assertTrue(clave.matches(".*[a-z].*"));
        assertTrue(clave.matches(".*[0-9].*"));
        assertTrue(clave.matches(".*[!@#$%].*"));
        assertTrue(Boolean.TRUE.equals(usuario.getDebeCambiarClave()));
        verify(repository).save(usuario);
    }
}
