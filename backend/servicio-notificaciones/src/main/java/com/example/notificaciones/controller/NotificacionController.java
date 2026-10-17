package com.example.notificaciones.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.notificaciones.dto.CrearEventoNotificacionRequest;
import com.example.notificaciones.dto.NotificacionDTO;
import com.example.notificaciones.dto.PageResponse;
import com.example.notificaciones.service.NotificacionService;
import com.example.notificaciones.service.ProveedorNotificacion;
import com.example.notificaciones.dto.CertificarProveedorRequest;
import com.example.notificaciones.dto.CertificacionProveedorDTO;
import java.time.LocalDateTime;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class NotificacionController {
    private final NotificacionService service;
    private final String expectedKey;
    private final ProveedorNotificacion proveedor;

    public NotificacionController(NotificacionService service, ProveedorNotificacion proveedor,
                                  @Value("${app.internal-api-key}") String expectedKey) {
        this.service = service;
        this.proveedor = proveedor;
        this.expectedKey = expectedKey;
    }

    @PostMapping("/internal/notificaciones")
    public List<NotificacionDTO> registrar(
            @Valid @RequestBody CrearEventoNotificacionRequest request,
            @RequestHeader("X-Internal-Api-Key") String providedKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationHeader) {
        validarClave(providedKey);
        String correlationId = correlationHeader == null ? MDC.get("traceId") : correlationHeader;
        return service.registrar(request, correlationId);
    }

    @GetMapping("/notificaciones")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR', 'RESPONSABLE_CONVIVENCIA')")
    public PageResponse<NotificacionDTO> buscar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String canal,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Long estudianteId,
            @PageableDefault(size = 25, sort = "creadaEn", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return service.buscar(estado, canal, tipo, estudianteId, pageable);
    }

    @PostMapping("/notificaciones/{id}/reintentar")
    @PreAuthorize("hasAnyRole('DIRECTOR')")
    public NotificacionDTO reintentar(@PathVariable Long id) {
        return service.reintentar(id);
    }

    @PostMapping("/notificaciones/proveedores/certificar")
    @PreAuthorize("hasAnyRole('DIRECTOR')")
    public CertificacionProveedorDTO certificar(
            @Valid @RequestBody CertificarProveedorRequest request) {
        String resultado = proveedor.certificar(request.canal(), request.destinatario());
        return new CertificacionProveedorDTO(request.canal().trim().toUpperCase(),
                proveedor.modo(), resultado, LocalDateTime.now());
    }

    private void validarClave(String providedKey) {
        if (!MessageDigest.isEqual(expectedKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new org.springframework.security.access.AccessDeniedException("Clave interna inválida");
        }
    }
}
