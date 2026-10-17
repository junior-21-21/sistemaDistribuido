package com.example.reportes.exception;

import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.beans.TypeMismatchException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
            "error", "Acceso denegado",
            "timestamp", LocalDateTime.now().toString(),
            "servicio", "servicio-reportes"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralExceptions(Exception ex) {
        // Errores del cliente (ruta inexistente, parámetro faltante o mal tipado, JSON inválido,
        // método no permitido, ResponseStatusException) conservan su código 4xx.
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(errorResponse.getStatusCode()).body(Map.of(
                    "error", "Petición inválida",
                    "message", mensajeCliente(ex)));
        }
        if (ex instanceof TypeMismatchException || ex instanceof HttpMessageNotReadableException) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Petición inválida",
                    "message", "Algún dato enviado no tiene el formato esperado"));
        }
        LOG.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Ocurrió un error inesperado en el servidor",
                "message", "Intenta nuevamente; si persiste, avisa al administrador"));
    }

    private static String mensajeCliente(Exception ex) {
        if (ex instanceof NoResourceFoundException) return "El recurso solicitado no existe";
        if (ex instanceof MissingServletRequestParameterException missing) {
            return "Falta el parámetro obligatorio '" + missing.getParameterName() + "'";
        }
        if (ex instanceof HttpRequestMethodNotSupportedException) return "Operación no permitida en esta ruta";
        if (ex instanceof ResponseStatusException status && status.getReason() != null) return status.getReason();
        return "La petición no es válida";
    }
}
