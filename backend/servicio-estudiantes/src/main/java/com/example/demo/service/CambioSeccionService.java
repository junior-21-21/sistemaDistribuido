package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.CambioSeccion;
import com.example.demo.model.Estudiante;
import com.example.demo.repository.CambioSeccionRepository;
import com.example.demo.repository.EstudianteRepository;
import com.example.demo.repository.MatriculaRepository;

/**
 * Cambio de sección dentro del mismo grado y año escolar. La matrícula del año pasa a la nueva
 * sección; la asistencia y las notas ya registradas se quedan como historial en la sección anterior.
 */
@Service
@Transactional(readOnly = true)
public class CambioSeccionService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    public record SolicitudCambio(Long seccionDestinoId, String motivo) {
    }

    private final EstudianteRepository estudiantes;
    private final MatriculaRepository matriculas;
    private final CambioSeccionRepository cambios;
    private final SeccionClient secciones;

    public CambioSeccionService(EstudianteRepository estudiantes, MatriculaRepository matriculas,
                                CambioSeccionRepository cambios, SeccionClient secciones) {
        this.estudiantes = estudiantes;
        this.matriculas = matriculas;
        this.cambios = cambios;
        this.secciones = secciones;
    }

    public List<CambioSeccion> historial(Long estudianteId) {
        return cambios.findByEstudianteIdOrderByFechaDesc(estudianteId);
    }

    @Transactional
    public CambioSeccion cambiar(Long estudianteId, SolicitudCambio solicitud, String usuario, String authorization) {
        if (solicitud == null || solicitud.seccionDestinoId() == null) {
            throw new IllegalArgumentException("Elige la sección de destino");
        }
        String motivo = solicitud.motivo() == null ? "" : solicitud.motivo().trim();
        if (motivo.length() < 5) {
            throw new IllegalArgumentException("Indica el motivo del cambio de sección");
        }
        Estudiante e = estudiantes.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        if (!"ACTIVO".equals(e.getEstado())) {
            throw new IllegalArgumentException("Solo se cambia de sección a estudiantes activos");
        }
        Long destinoId = solicitud.seccionDestinoId();
        if (Objects.equals(e.getSeccionId(), destinoId)) {
            throw new IllegalArgumentException("El estudiante ya está en esa sección");
        }
        SeccionClient.Seccion destino = secciones.obtener(destinoId, authorization);
        if (destino == null || Boolean.FALSE.equals(destino.activa())) {
            throw new IllegalArgumentException("La sección de destino no está activa");
        }
        if (e.getGradoId() != null && !e.getGradoId().equals(destino.gradoId())) {
            throw new IllegalArgumentException("La sección de destino es de otro grado; para cambiar de grado usa la promoción");
        }
        int anio = TrayectoriaService.anioActual();
        if (destino.anioEscolar() != null && destino.anioEscolar() != anio) {
            throw new IllegalArgumentException("La sección de destino no es del año escolar " + anio);
        }
        long ocupados = estudiantes.findBySeccionIdAndEstadoOrderByApellidoAscNombreAsc(destinoId, "ACTIVO").size();
        if (destino.capacidadMaxima() != null && ocupados >= destino.capacidadMaxima()) {
            throw new IllegalArgumentException("La sección " + destino.nombre() + " no tiene vacantes ("
                    + ocupados + " de " + destino.capacidadMaxima() + ")");
        }

        CambioSeccion cambio = new CambioSeccion();
        cambio.setEstudianteId(estudianteId);
        cambio.setAnioEscolar(anio);
        cambio.setSeccionOrigenId(e.getSeccionId());
        cambio.setSeccionDestinoId(destinoId);
        cambio.setMotivo(motivo.length() > 300 ? motivo.substring(0, 300) : motivo);
        cambio.setRegistradoPor(usuario);
        cambio.setFecha(LocalDateTime.now(LIMA));

        e.setSeccionId(destinoId);
        estudiantes.save(e);
        matriculas.findByEstudianteIdAndAnioEscolar(estudianteId, anio).ifPresent(m -> {
            m.setSeccionId(destinoId);
            matriculas.save(m);
        });
        return cambios.save(cambio);
    }
}
