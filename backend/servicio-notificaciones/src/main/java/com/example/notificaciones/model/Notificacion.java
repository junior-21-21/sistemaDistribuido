package com.example.notificaciones.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "notificaciones", uniqueConstraints =
        @UniqueConstraint(name = "uk_notificacion_evento_canal", columnNames = {"evento_id", "canal"}))
public class Notificacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "evento_id", nullable = false, length = 36) private String eventoId;
    @Column(nullable = false, length = 40) private String tipo;
    @Column(nullable = false, length = 20) private String canal;
    @Column(name = "estudiante_id", nullable = false) private Long estudianteId;
    @Column(nullable = false, length = 180) private String asunto;
    @Column(nullable = false, length = 2000) private String mensaje;
    @Column(length = 320) private String destinatario;
    @Column(nullable = false, length = 30) private String estado;
    @Column(nullable = false) private int intentos;
    @Column(name = "max_intentos", nullable = false) private int maxIntentos;
    @Column(name = "proximo_intento") private LocalDateTime proximoIntento;
    @Column(name = "ultimo_error", length = 1000) private String ultimoError;
    @Column(name = "correlation_id", length = 100) private String correlationId;
    @Column(name = "detalle_proveedor", length = 200) private String detalleProveedor;
    @Column(name = "creada_en", nullable = false) private LocalDateTime creadaEn;
    @Column(name = "actualizada_en", nullable = false) private LocalDateTime actualizadaEn;
    @Column(name = "enviada_en") private LocalDateTime enviadaEn;

    @PrePersist void crearFechas() { creadaEn = actualizadaEn = LocalDateTime.now(); }
    @PreUpdate void actualizarFecha() { actualizadaEn = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public Long getEstudianteId() { return estudianteId; }
    public void setEstudianteId(Long estudianteId) { this.estudianteId = estudianteId; }
    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int intentos) { this.intentos = intentos; }
    public int getMaxIntentos() { return maxIntentos; }
    public void setMaxIntentos(int maxIntentos) { this.maxIntentos = maxIntentos; }
    public LocalDateTime getProximoIntento() { return proximoIntento; }
    public void setProximoIntento(LocalDateTime proximoIntento) { this.proximoIntento = proximoIntento; }
    public String getUltimoError() { return ultimoError; }
    public void setUltimoError(String ultimoError) { this.ultimoError = ultimoError; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getDetalleProveedor() { return detalleProveedor; }
    public void setDetalleProveedor(String detalleProveedor) { this.detalleProveedor = detalleProveedor; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public LocalDateTime getActualizadaEn() { return actualizadaEn; }
    public LocalDateTime getEnviadaEn() { return enviadaEn; }
    public void setEnviadaEn(LocalDateTime enviadaEn) { this.enviadaEn = enviadaEn; }
}
