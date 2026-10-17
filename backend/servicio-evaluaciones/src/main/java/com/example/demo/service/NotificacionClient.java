package com.example.demo.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificacionClient {
    private final RestClient restClient; private final JdbcTemplate jdbc;
    private final String internalKey; private final boolean enabled;

    public NotificacionClient(RestClient.Builder builder, JdbcTemplate jdbc,
            @Value("${services.notificaciones-url:http://servicio-notificaciones:8086}") String baseUrl,
            @Value("${app.internal-api-key}") String internalKey,
            @Value("${notifications.enabled:false}") boolean enabled) {
        this.restClient=builder.baseUrl(baseUrl).build(); this.jdbc=jdbc;
        this.internalKey=internalKey; this.enabled=enabled;
    }

    public void inasistencia(Long asistenciaId,Long estudianteId,String estado,String fecha) {
        if(!enabled||!Set.of("FALTA","TARDE").contains(estado)) return;
        encolar("ASISTENCIA-"+asistenciaId+"-"+estado,"INASISTENCIA",estudianteId,
                "Alerta de asistencia","Se registró "+estado.toLowerCase()+" para el estudiante el "+fecha+".");
    }

    private void encolar(String eventoId,String tipo,Long estudianteId,String asunto,String mensaje) {
        jdbc.update("""
            INSERT INTO notification_outbox(evento_id,tipo,estudiante_id,asunto,mensaje,estado,intentos,
            proximo_intento,correlation_id,creado_en) VALUES (?,?,?,?,?,'PENDIENTE',0,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP)
            ON CONFLICT (evento_id) DO NOTHING""",eventoId,tipo,estudianteId,asunto,mensaje,MDC.get("traceId"));
    }

    @Scheduled(fixedDelayString="${notifications.outbox-publish-ms:5000}")
    public void publicarPendientes(){if(enabled) pendientes().forEach(this::publicar);}
    private List<Evento> pendientes(){return jdbc.query("""
        SELECT id,evento_id,tipo,estudiante_id,asunto,mensaje,
        intentos,correlation_id FROM notification_outbox WHERE estado='PENDIENTE' AND proximo_intento<=CURRENT_TIMESTAMP
        ORDER BY id LIMIT 50""",(rs,row)->new Evento(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getLong(4),
        rs.getString(5),rs.getString(6),rs.getInt(7),rs.getString(8)));}
    private void publicar(Evento e){try{var body=Map.of("eventoId",e.eventoId(),"tipo",e.tipo(),
        "estudianteId",e.estudianteId(),"asunto",e.asunto(),"mensaje",e.mensaje(),"canales",Set.of("EMAIL","SMS","WHATSAPP"));
        var spec=restClient.post().uri("/api/internal/notificaciones").header("X-Internal-Api-Key",internalKey);
        if(e.correlationId()!=null)spec.header("X-Correlation-Id",e.correlationId());spec.body(body).retrieve().toBodilessEntity();
        jdbc.update("UPDATE notification_outbox SET estado='ENVIADO',enviado_en=CURRENT_TIMESTAMP WHERE id=?",e.id());
        }catch(Exception ex){fallo(e,ex);}}
    private void fallo(Evento e,Exception ex){int intentos=e.intentos()+1;String estado=intentos>=10?"FALLIDO":"PENDIENTE";
        String error=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();jdbc.update(
        "UPDATE notification_outbox SET estado=?,intentos=?,proximo_intento=?,ultimo_error=? WHERE id=?",estado,intentos,
        Timestamp.valueOf(LocalDateTime.now().plusSeconds(Math.min(300,1L<<Math.min(intentos,8)))),
        error.substring(0,Math.min(1000,error.length())),e.id());}
    private record Evento(Long id,String eventoId,String tipo,Long estudianteId,String asunto,String mensaje,int intentos,String correlationId){}
}
