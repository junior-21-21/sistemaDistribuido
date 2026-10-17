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
    public NotificacionClient(RestClient.Builder builder,JdbcTemplate jdbc,
            @Value("${services.notificaciones-url:http://servicio-notificaciones:8086}")String baseUrl,
            @Value("${app.internal-api-key}")String internalKey,@Value("${notifications.enabled:false}")boolean enabled){
        this.restClient=builder.baseUrl(baseUrl).build();this.jdbc=jdbc;this.internalKey=internalKey;this.enabled=enabled;}
    public void derivacion(Long id,Long estudianteId,String motivo,String prioridad){if(!enabled)return;encolar(
        "DERIVACION-"+id,"DERIVACION",estudianteId,"Derivación del equipo de orientación",
        "Motivo: "+motivo+". Prioridad: "+prioridad+".");}
    public void ingreso(Long id,Long estudianteId,String nombre,java.time.LocalTime hora,boolean tardanza){if(!enabled)return;
        String h=hora.toString().substring(0,5);encolar("INGRESO-"+id,"INGRESO",estudianteId,
        tardanza?"Llegada tarde al colegio":"Llegada al colegio",
        nombre+(tardanza?" llegó tarde al colegio a las ":" llegó al colegio a las ")+h+".");}
    public void topico(Long id,Long estudianteId,String motivo,String destino,String establecimiento){if(!enabled)return;
        String detalle=switch(destino){case "TRASLADO_SALUD"->" Fue trasladado(a) a "+establecimiento+".";
            case "RETIRADO_APODERADO"->" Se coordinó su retiro con la familia.";default->" Retornó a clases.";};
        encolar("TOPICO-"+id,"TOPICO",estudianteId,"Atención en el tópico del colegio",
        "Su hijo(a) fue atendido(a) en el tópico por: "+motivo+"."+detalle);}
    public void permiso(Long id,Long estudianteId,String tipo,String estado,String observacion){if(!enabled)return;
        String nombre=switch(tipo){case "JUSTIFICACION"->"La justificación de inasistencia";
            case "PAPELETA_SALIDA"->"La papeleta de salida";default->"El permiso";};
        encolar("PERMISO-"+id+"-"+estado,"PERMISO",estudianteId,nombre+(("APROBADA".equals(estado))?" fue aprobada":" no fue aprobada"),
        nombre+("APROBADA".equals(estado)?" fue aprobada.":" no fue aprobada.")+(observacion==null||observacion.isBlank()?"":" Observación: "+observacion));}
    private void encolar(String eventoId,String tipo,Long estudianteId,String asunto,String mensaje){jdbc.update("""
        INSERT INTO notification_outbox(evento_id,tipo,estudiante_id,asunto,mensaje,estado,intentos,proximo_intento,
        correlation_id,creado_en) VALUES (?,?,?,?,?,'PENDIENTE',0,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP)
        ON CONFLICT (evento_id) DO NOTHING""",eventoId,tipo,estudianteId,asunto,mensaje,MDC.get("traceId"));}
    @Scheduled(fixedDelayString="${notifications.outbox-publish-ms:5000}")
    public void publicarPendientes(){if(enabled)pendientes().forEach(this::publicar);}
    private List<Evento> pendientes(){return jdbc.query("""
        SELECT id,evento_id,tipo,estudiante_id,asunto,mensaje,
        intentos,correlation_id FROM notification_outbox WHERE estado='PENDIENTE' AND proximo_intento<=CURRENT_TIMESTAMP
        ORDER BY id LIMIT 50""",(rs,row)->new Evento(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getLong(4),
        rs.getString(5),rs.getString(6),rs.getInt(7),rs.getString(8)));}
    private void publicar(Evento e){try{var body=Map.of("eventoId",e.eventoId(),"tipo",e.tipo(),"estudianteId",e.estudianteId(),
        "asunto",e.asunto(),"mensaje",e.mensaje(),"canales",Set.of("EMAIL","SMS","WHATSAPP"));var spec=restClient.post()
        .uri("/api/internal/notificaciones").header("X-Internal-Api-Key",internalKey);if(e.correlationId()!=null)
        spec.header("X-Correlation-Id",e.correlationId());spec.body(body).retrieve().toBodilessEntity();jdbc.update(
        "UPDATE notification_outbox SET estado='ENVIADO',enviado_en=CURRENT_TIMESTAMP WHERE id=?",e.id());}catch(Exception ex){fallo(e,ex);}}
    private void fallo(Evento e,Exception ex){int n=e.intentos()+1;String error=ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();
        jdbc.update("UPDATE notification_outbox SET estado=?,intentos=?,proximo_intento=?,ultimo_error=? WHERE id=?",
        n>=10?"FALLIDO":"PENDIENTE",n,Timestamp.valueOf(LocalDateTime.now().plusSeconds(Math.min(300,1L<<Math.min(n,8)))),
        error.substring(0,Math.min(1000,error.length())),e.id());}
    private record Evento(Long id,String eventoId,String tipo,Long estudianteId,String asunto,String mensaje,int intentos,String correlationId){}
}
