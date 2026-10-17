package com.example.notificaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ProveedorNotificacionTest {

    @Test
    void modoSimuladoNoRequiereCredencialesExternas() {
        ProveedorNotificacion proveedor = proveedor("SIMULATED", "", false, "EMAIL,SMS,WHATSAPP");

        proveedor.validarConfiguracion();

        assertEquals("SIMULATED", proveedor.enviar(
                "EVENTO-1", "EMAIL", "familia@example.test", "Aviso", "Mensaje"));
    }

    @Test
    void modoWebhookExigeTodosLosCanalesDeclarados() {
        ProveedorNotificacion proveedor = proveedor("WEBHOOK", "", false, "EMAIL");

        assertThrows(IllegalStateException.class, proveedor::validarConfiguracion);
    }

    @Test
    void webhookRechazaHttpNoSeguro() {
        ProveedorNotificacion proveedor = proveedor(
                "WEBHOOK", "http://localhost/email", false, "EMAIL");

        assertThrows(IllegalStateException.class, proveedor::validarConfiguracion);
    }

    @Test
    void modoWebhookExigeTokenBearer() {
        ProveedorNotificacion proveedor = new ProveedorNotificacion(RestClient.builder(), "WEBHOOK",
                "https://proveedor.example.test/email", "", "", "", "EMAIL", false, 1000, 1000);

        assertThrows(IllegalStateException.class, proveedor::validarConfiguracion);
    }

    private ProveedorNotificacion proveedor(
            String modo, String email, boolean permitirHttp, String requeridos) {
        return new ProveedorNotificacion(RestClient.builder(), modo, email, "", "", "token",
                requeridos, permitirHttp, 1000, 1000);
    }
}
