package com.example.notificaciones.dto;

import java.time.LocalDateTime;
import com.example.notificaciones.model.Notificacion;

public record NotificacionDTO(
        Long id, String eventoId, String tipo, String canal, Long estudianteId,
        String asunto, String destinatario, String estado, int intentos, int maxIntentos,
        LocalDateTime proximoIntento, String ultimoError, String correlationId,
        String detalleProveedor, LocalDateTime creadaEn, LocalDateTime enviadaEn) {
    public static NotificacionDTO from(Notificacion n) {
        return new NotificacionDTO(n.getId(), n.getEventoId(), n.getTipo(), n.getCanal(), n.getEstudianteId(),
                n.getAsunto(), n.getDestinatario(), n.getEstado(), n.getIntentos(), n.getMaxIntentos(),
                n.getProximoIntento(), n.getUltimoError(), n.getCorrelationId(), n.getDetalleProveedor(),
                n.getCreadaEn(), n.getEnviadaEn());
    }
}
