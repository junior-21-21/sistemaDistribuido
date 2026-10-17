package com.example.gateway.tiemporeal;

import java.time.Instant;

/**
 * Aviso de que un módulo cambió. No transporta datos del registro: el cliente vuelve a
 * consultar la API con sus propios permisos, así el canal no puede filtrar información.
 */
public record EventoCambio(
        String id,
        String modulo,
        String accion,
        String descripcion,
        String actor,
        String actorUsuario,
        Instant fecha) {
}
