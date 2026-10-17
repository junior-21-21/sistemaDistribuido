package com.example.notificaciones.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ContactoClient {
    public record Contacto(Long apoderadoId, Long estudianteId, String nombre, String parentesco,
                           String email, String telefono) {}

    private final RestClient client;
    private final String internalKey;

    public ContactoClient(RestClient.Builder builder,
                          @Value("${services.estudiantes-url}") String estudiantesUrl,
                          @Value("${app.internal-api-key}") String internalKey) {
        this.client = builder.baseUrl(estudiantesUrl).build();
        this.internalKey = internalKey;
    }

    public List<Contacto> obtener(Long estudianteId) {
        return client.get().uri("/api/internal/contactos/estudiante/{id}", estudianteId)
                .header("X-Internal-Api-Key", internalKey)
                .retrieve().body(new ParameterizedTypeReference<List<Contacto>>() {});
    }
}
