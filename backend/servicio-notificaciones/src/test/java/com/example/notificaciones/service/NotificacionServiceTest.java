package com.example.notificaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.example.notificaciones.config.RabbitConfig;
import com.example.notificaciones.dto.CrearEventoNotificacionRequest;
import com.example.notificaciones.model.Notificacion;
import com.example.notificaciones.repository.NotificacionRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {
    @Mock NotificacionRepository repository;
    @Mock RabbitTemplate rabbitTemplate;
    @Mock ContactoClient contactoClient;
    @Mock ProveedorNotificacion proveedor;
    NotificacionService service;

    @BeforeEach
    void setUp() {
        service = new NotificacionService(repository, rabbitTemplate, contactoClient,
                proveedor, new SimpleMeterRegistry());
    }

    @Test
    void persisteUnaEntregaPorCanalAntesDePublicar() {
        AtomicLong sequence = new AtomicLong(1);
        when(repository.findByEventoIdAndCanal(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> {
            Notificacion item = invocation.getArgument(0);
            item.setId(sequence.getAndIncrement());
            return item;
        });

        var request = new CrearEventoNotificacionRequest("ASISTENCIA-10-FALTA", "INASISTENCIA", 5L,
                "Alerta de asistencia", "Se registró una falta.", Set.of("EMAIL", "SMS", "WHATSAPP"));
        var result = service.registrar(request, "trace-123");

        assertEquals(3, result.size());
        verify(repository, times(3)).save(any(Notificacion.class));
        verify(rabbitTemplate, times(3)).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitConfig.EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitConfig.ROUTING_KEY), any(Long.class));
    }

    @Test
    void entregaSimuladaResuelveElContactoYMarcaEnviada() {
        Notificacion item = nuevaNotificacion();
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        when(contactoClient.obtener(5L)).thenReturn(List.of(
                new ContactoClient.Contacto(8L, 5L, "Familia", "MADRE", "familia@correo.pe", "999111222")));
        when(proveedor.enviar("EVENTO-1", "EMAIL", "familia@correo.pe", "Aviso", "Mensaje"))
                .thenReturn("SIMULATED");

        service.procesar(1L);

        assertEquals("ENVIADA", item.getEstado());
        assertEquals(1, item.getIntentos());
        assertEquals("familia@correo.pe", item.getDestinatario());
        verify(repository).save(item);
    }

    private Notificacion nuevaNotificacion() {
        Notificacion item = new Notificacion();
        item.setId(1L); item.setEventoId("EVENTO-1"); item.setTipo("INASISTENCIA");
        item.setCanal("EMAIL"); item.setEstudianteId(5L); item.setAsunto("Aviso");
        item.setMensaje("Mensaje"); item.setEstado("PENDIENTE"); item.setMaxIntentos(5);
        return item;
    }
}
