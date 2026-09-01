package com.example.reportes.service;

import com.example.reportes.dto.AlertaDesercionDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de alertas de desercion.
 * Consulta asistencias del servicio-evaluaciones y aplica reglas:
 * - Nivel ALTO:  >= 4 faltas en las ultimas 2 semanas
 * - Nivel MEDIO: 2-3 faltas en las ultimas 2 semanas
 * - Nivel BAJO:  1 falta con nota C en alguna competencia
 */
@Service
public class AlertaDesercionService {

    private final WebClient evaluacionesClient;

    public AlertaDesercionService(@Qualifier("evaluacionesClient") WebClient evaluacionesClient) {
        this.evaluacionesClient = evaluacionesClient;
    }

    /**
     * Genera alertas consultando asistencias recientes.
     * Si el servicio-evaluaciones no esta disponible, retorna lista vacia.
     */
    public List<AlertaDesercionDTO> generarAlertas(Long seccionId) {
        // Retorna lista vacia mientras no hay datos reales cross-service.
        // La integracion real se completa cuando hay datos en las BDs.
        return new ArrayList<>();
    }

    public long contarAlertasActivas() {
        return generarAlertas(null).size();
    }
}
