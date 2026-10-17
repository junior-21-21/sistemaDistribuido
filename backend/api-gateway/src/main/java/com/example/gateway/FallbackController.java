package com.example.gateway;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Respuesta cuando un microservicio no responde (caído, reiniciando o circuito abierto).
 * Devuelve 503 con un mensaje claro en lugar de un 500 genérico.
 */
@RestController
public class FallbackController {

    private static final Map<String, String> NOMBRES = Map.of(
            "servicio-estudiantes", "estudiantes y cuentas",
            "servicio-cursos", "cursos y horarios",
            "servicio-evaluaciones", "notas y asistencia",
            "servicio-disciplina", "convivencia",
            "servicio-reportes", "reportes",
            "servicio-notificaciones", "notificaciones",
            "servicio-horarios", "generador de horarios");

    @RequestMapping("/fallback/{servicio}")
    public ResponseEntity<Map<String, String>> noDisponible(@PathVariable String servicio) {
        String nombre = NOMBRES.getOrDefault(servicio, "solicitado");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "SERVICE_UNAVAILABLE",
                "message", "El servicio de " + nombre + " no está disponible en este momento. Reintenta en unos segundos."));
    }
}
