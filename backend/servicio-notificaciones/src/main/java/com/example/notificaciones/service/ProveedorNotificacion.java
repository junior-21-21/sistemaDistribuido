package com.example.notificaciones.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProveedorNotificacion {
    private static final Logger log = LoggerFactory.getLogger(ProveedorNotificacion.class);
    private final String modo;
    private final Map<String, String> webhooks;
    private final String bearerToken;
    private final Set<String> canalesRequeridos;
    private final boolean permitirHttp;
    private final RestClient client;

    public ProveedorNotificacion(RestClient.Builder builder,
            @Value("${notifications.mode:SIMULATED}") String modo,
            @Value("${notifications.email.webhook-url:}") String email,
            @Value("${notifications.sms.webhook-url:}") String sms,
            @Value("${notifications.whatsapp.webhook-url:}") String whatsapp,
            @Value("${notifications.webhook-token:}") String bearerToken,
            @Value("${notifications.required-channels:EMAIL,SMS,WHATSAPP}") String requiredChannels,
            @Value("${notifications.allow-insecure-http:false}") boolean permitirHttp,
            @Value("${notifications.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${notifications.read-timeout-ms:10000}") int readTimeoutMs) {
        this.modo = modo.trim().toUpperCase(Locale.ROOT);
        this.webhooks = Map.of("EMAIL", email, "SMS", sms, "WHATSAPP", whatsapp);
        this.bearerToken = bearerToken;
        this.canalesRequeridos = Arrays.stream(requiredChannels.split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        this.permitirHttp = permitirHttp;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.client = builder.requestFactory(requestFactory).build();
    }

    @PostConstruct
    void validarConfiguracion() {
        if (!Set.of("SIMULATED", "WEBHOOK").contains(modo)) {
            throw new IllegalStateException("NOTIFICATIONS_MODE debe ser SIMULATED o WEBHOOK");
        }
        if ("SIMULATED".equals(modo)) return;
        for (String canal : canalesRequeridos) {
            String webhook = webhooks.getOrDefault(canal, "");
            if (webhook.isBlank()) {
                throw new IllegalStateException("Falta configurar el webhook del canal " + canal);
            }
            URI uri = URI.create(webhook);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    && !(permitirHttp && "http".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalStateException("El webhook de " + canal + " debe usar HTTPS");
            }
        }
        if (bearerToken.isBlank()) {
            throw new IllegalStateException("NOTIFICATIONS_WEBHOOK_TOKEN es obligatorio en modo WEBHOOK");
        }
    }

    public String enviar(String eventoId, String canal, String destinatario, String asunto, String mensaje) {
        if ("SIMULATED".equalsIgnoreCase(modo)) {
            log.info("Entrega simulada evento={} canal={} destino={}", eventoId, canal, ocultar(destinatario));
            return "SIMULATED";
        }
        String webhook = webhooks.getOrDefault(canal, "");
        if (webhook.isBlank()) throw new IllegalStateException("Proveedor " + canal + " no configurado");
        RestClient.RequestBodySpec request = client.post().uri(webhook)
                .header("Idempotency-Key", eventoId + '-' + canal);
        if (!bearerToken.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        }
        var response = request.body(Map.of("eventId", eventoId, "channel", canal,
                        "to", destinatario, "subject", asunto, "message", mensaje))
                .retrieve().toBodilessEntity();
        return "WEBHOOK:" + response.getStatusCode().value();
    }

    public String modo() {
        return modo;
    }

    public Set<String> canalesConfigurados() {
        return webhooks.entrySet().stream().filter(entry -> !entry.getValue().isBlank())
                .map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet());
    }

    public String certificar(String canal, String destinatario) {
        String canalNormalizado = canal.trim().toUpperCase(Locale.ROOT);
        if (!webhooks.containsKey(canalNormalizado)) {
            throw new IllegalArgumentException("Canal de notificación inválido");
        }
        String eventoId = "CERTIFICACION-" + java.util.UUID.randomUUID();
        return enviar(eventoId, canalNormalizado, destinatario,
                "Prueba de canal institucional",
                "Mensaje técnico de certificación. No requiere respuesta.");
    }

    private String ocultar(String value) {
        if (value == null || value.length() < 5) return "***";
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}
