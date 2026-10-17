package com.example.notificaciones.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.notificaciones.config.RabbitConfig;
import com.example.notificaciones.dto.CrearEventoNotificacionRequest;
import com.example.notificaciones.dto.NotificacionDTO;
import com.example.notificaciones.dto.PageResponse;
import com.example.notificaciones.model.Notificacion;
import com.example.notificaciones.repository.NotificacionRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class NotificacionService {
    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final Set<String> CANALES = Set.of("EMAIL", "SMS", "WHATSAPP");

    private final NotificacionRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ContactoClient contactoClient;
    private final ProveedorNotificacion proveedor;
    private final Counter enviadas;
    private final Counter fallidas;

    public NotificacionService(NotificacionRepository repository, RabbitTemplate rabbitTemplate,
            ContactoClient contactoClient, ProveedorNotificacion proveedor, MeterRegistry registry) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.contactoClient = contactoClient;
        this.proveedor = proveedor;
        this.enviadas = registry.counter("pad_notificaciones_enviadas_total");
        this.fallidas = registry.counter("pad_notificaciones_fallidas_total");
    }

    @Transactional
    public List<NotificacionDTO> registrar(CrearEventoNotificacionRequest request, String correlationId) {
        Set<String> canales = request.canales() == null || request.canales().isEmpty()
                ? CANALES : request.canales().stream().map(this::normalizarCanal).collect(java.util.stream.Collectors.toSet());
        List<Notificacion> registros = canales.stream().map(canal ->
                repository.findByEventoIdAndCanal(request.eventoId(), canal).orElseGet(() -> {
                    Notificacion n = new Notificacion();
                    n.setEventoId(request.eventoId());
                    n.setTipo(request.tipo().trim().toUpperCase(Locale.ROOT));
                    n.setCanal(canal);
                    n.setEstudianteId(request.estudianteId());
                    n.setAsunto(request.asunto().trim());
                    n.setMensaje(request.mensaje().trim());
                    n.setEstado("PENDIENTE");
                    n.setIntentos(0);
                    n.setMaxIntentos(5);
                    n.setCorrelationId(correlationId);
                    return repository.save(n);
                })).toList();
        registros.forEach(this::publicar);
        return registros.stream().map(NotificacionDTO::from).toList();
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    @Transactional
    public void procesar(Long id) {
        Notificacion n = repository.findById(id).orElse(null);
        if (n == null || "ENVIADA".equals(n.getEstado()) || "OMITIDA".equals(n.getEstado())) return;
        try {
            n.setIntentos(n.getIntentos() + 1);
            String destino = resolverDestino(n);
            if (destino == null) {
                n.setEstado("OMITIDA");
                n.setUltimoError("El apoderado no tiene un dato de contacto válido para " + n.getCanal());
                repository.save(n);
                return;
            }
            n.setDestinatario(destino);
            n.setDetalleProveedor(proveedor.enviar(
                    n.getEventoId(), n.getCanal(), destino, n.getAsunto(), n.getMensaje()));
            n.setEstado("ENVIADA");
            n.setEnviadaEn(LocalDateTime.now());
            n.setProximoIntento(null);
            n.setUltimoError(null);
            repository.save(n);
            enviadas.increment();
        } catch (Exception ex) {
            gestionarFallo(n, ex);
        }
    }

    @Scheduled(fixedDelayString = "${notifications.retry-scan-ms:30000}")
    @Transactional(readOnly = true)
    public void republicarPendientes() {
        repository.buscarReintentos(LocalDateTime.now()).forEach(this::publicar);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificacionDTO> buscar(String estado, String canal, String tipo,
                                                 Long estudianteId, Pageable pageable) {
        var page = repository.buscar(normalizar(estado), normalizar(canal), normalizar(tipo), estudianteId, pageable)
                .map(NotificacionDTO::from);
        return PageResponse.from(page);
    }

    @Transactional
    public NotificacionDTO reintentar(Long id) {
        Notificacion n = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));
        n.setEstado("REINTENTO");
        n.setProximoIntento(LocalDateTime.now());
        n.setUltimoError(null);
        repository.save(n);
        publicar(n);
        return NotificacionDTO.from(n);
    }

    private void gestionarFallo(Notificacion n, Exception ex) {
        String error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        n.setUltimoError(error.substring(0, Math.min(error.length(), 1000)));
        if (n.getIntentos() >= n.getMaxIntentos()) {
            n.setEstado("FALLIDA");
            n.setProximoIntento(null);
            repository.save(n);
            rabbitTemplate.convertAndSend("", RabbitConfig.DLQ, n.getId());
            fallidas.increment();
            log.error("Notificación {} agotó sus reintentos", n.getId(), ex);
        } else {
            long demoraMinutos = Math.min(60, 1L << Math.min(n.getIntentos(), 6));
            n.setEstado("REINTENTO");
            n.setProximoIntento(LocalDateTime.now().plusMinutes(demoraMinutos));
            repository.save(n);
            log.warn("Notificación {} se reintentará en {} minutos", n.getId(), demoraMinutos);
        }
    }

    private String resolverDestino(Notificacion n) {
        var contactos = contactoClient.obtener(n.getEstudianteId());
        return contactos.stream().map(c -> "EMAIL".equals(n.getCanal()) ? c.email() : c.telefono())
                .filter(v -> v != null && !v.isBlank()).findFirst().orElse(null);
    }

    private void publicar(Notificacion n) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, n.getId());
    }

    private String normalizarCanal(String value) {
        String canal = normalizar(value);
        if (!CANALES.contains(canal)) throw new IllegalArgumentException("Canal de notificación inválido");
        return canal;
    }

    private String normalizar(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
