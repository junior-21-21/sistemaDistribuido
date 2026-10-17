package com.example.demo.service;

import com.example.demo.dto.AsistenciaDTO;
import com.example.demo.dto.SesionAsistenciaDTO;
import com.example.demo.mapper.AsistenciaMapper;
import com.example.demo.model.Asistencia;
import com.example.demo.model.SesionAsistencia;
import com.example.demo.repository.AsistenciaRepository;
import com.example.demo.repository.SesionAsistenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Locale;

import com.example.demo.dto.RiesgoAsistenciaDTO;
import java.util.stream.Collectors;

@Service
@Transactional
public class AsistenciaService {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private AsistenciaMapper asistenciaMapper;

    @Autowired
    private SesionAsistenciaRepository sesionRepository;

    @Autowired
    private NotificacionClient notificacionClient;

    public List<AsistenciaDTO> listarPorEstudiante(Long estudianteId) {
        return asistenciaRepository.findByEstudianteId(estudianteId).stream()
                .map(asistenciaMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<AsistenciaDTO> listarPorHorario(Long horarioId) {
        return asistenciaRepository.findByHorarioId(horarioId).stream()
                .map(asistenciaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AsistenciaDTO> listarPorSeccionYFecha(Long seccionId, LocalDate fecha) {
        return asistenciaRepository.findBySeccionIdAndFecha(seccionId, fecha).stream()
                .map(asistenciaMapper::toDto)
                .collect(Collectors.toList());
    }

    /** Registros de una sección (y opcionalmente de un curso) en un rango, para los reportes de día, semana o mes. */
    @Transactional(readOnly = true)
    public List<AsistenciaDTO> listarPorRango(Long seccionId, Long cursoId, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        if (desde.plusDays(62).isBefore(hasta)) throw new IllegalArgumentException("El reporte admite como máximo dos meses");
        List<Asistencia> registros = cursoId == null
                ? asistenciaRepository.findBySeccionIdAndFechaBetweenOrderByFechaAsc(seccionId, desde, hasta)
                : asistenciaRepository.findBySeccionIdAndCursoIdAndFechaBetweenOrderByFechaAsc(seccionId, cursoId, desde, hasta);
        return registros.stream().map(asistenciaMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AsistenciaDTO> listarHoy(Long seccionId, Long cursoId) {
        return asistenciaRepository.findBySeccionIdAndCursoIdAndFecha(
                        seccionId, cursoId, LocalDate.now()).stream()
                .map(asistenciaMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<RiesgoAsistenciaDTO> resumirFaltasRecientes(LocalDate desde, Long seccionId) {
        Map<Long, List<Asistencia>> porEstudiante = asistenciaRepository
                .findByEstadoAndFechaGreaterThanEqual("FALTA", desde).stream()
                .filter(asistencia -> seccionId == null || seccionId.equals(asistencia.getSeccionId()))
                .collect(Collectors.groupingBy(Asistencia::getEstudianteId));
        return porEstudiante.values().stream()
                .map(registros -> {
                    Asistencia ultima = registros.stream()
                            .max(Comparator.comparing(Asistencia::getFecha))
                            .orElseThrow();
                    // Se cuentan días con falta, no bloques de clase: faltar un día no son seis faltas
                    long dias = registros.stream().map(Asistencia::getFecha).distinct().count();
                    return new RiesgoAsistenciaDTO(
                            ultima.getEstudianteId(), ultima.getSeccionId(),
                            dias, ultima.getFecha());
                })
                .sorted(Comparator.comparingLong(RiesgoAsistenciaDTO::faltasRecientes).reversed())
                .toList();
    }

    public AsistenciaDTO guardar(AsistenciaDTO asistenciaDTO, String username) {
        validarRegistro(asistenciaDTO);
        asegurarSesionAbierta(asistenciaDTO, username);
        Asistencia asistencia = asistenciaRepository.findByEstudianteIdAndHorarioIdAndFecha(
                        asistenciaDTO.getEstudianteId(), asistenciaDTO.getHorarioId(), asistenciaDTO.getFecha())
                .orElseGet(Asistencia::new);
        copiar(asistenciaDTO, asistencia, username);
        Asistencia guardada = asistenciaRepository.save(asistencia);
        notificacionClient.inasistencia(guardada.getId(), guardada.getEstudianteId(),
                guardada.getEstado(), guardada.getFecha().toString());
        return asistenciaMapper.toDto(guardada);
    }

    public List<AsistenciaDTO> guardarTodas(List<AsistenciaDTO> asistenciasDTO, String username) {
        if (asistenciasDTO.isEmpty()) throw new IllegalArgumentException("Debe enviar al menos un registro");
        AsistenciaDTO contexto = asistenciasDTO.get(0);
        boolean mismaSesion = asistenciasDTO.stream().allMatch(dto ->
                contexto.getHorarioId().equals(dto.getHorarioId())
                && contexto.getFecha().equals(dto.getFecha())
                && contexto.getSeccionId().equals(dto.getSeccionId())
                && contexto.getCursoId().equals(dto.getCursoId()));
        if (!mismaSesion) throw new IllegalArgumentException("Todos los registros deben pertenecer a la misma sesión");
        asegurarSesionAbierta(contexto, username);
        List<Asistencia> asistencias = asistenciasDTO.stream().map(dto -> {
            validarRegistro(dto);
            Asistencia entity = asistenciaRepository.findByEstudianteIdAndHorarioIdAndFecha(
                    dto.getEstudianteId(), dto.getHorarioId(), dto.getFecha()).orElseGet(Asistencia::new);
            copiar(dto, entity, username);
            return entity;
        }).toList();
        List<Asistencia> guardadas = asistenciaRepository.saveAll(asistencias);
        guardadas.forEach(item -> notificacionClient.inasistencia(item.getId(), item.getEstudianteId(),
                item.getEstado(), item.getFecha().toString()));
        return guardadas.stream()
                .map(asistenciaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SesionAsistenciaDTO obtenerSesion(Long horarioId, LocalDate fecha) {
        return sesionRepository.findByHorarioIdAndFecha(horarioId, fecha).map(this::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public SesionAsistenciaDTO obtenerSesionPorId(Long id) {
        return sesionRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Sesión de asistencia no encontrada"));
    }

    public SesionAsistenciaDTO abrirSesion(SesionAsistenciaDTO dto, String username) {
        SesionAsistencia sesion = sesionRepository.findByHorarioIdAndFecha(dto.getHorarioId(), dto.getFecha())
                .orElseGet(SesionAsistencia::new);
        if (sesion.getId() != null && "CERRADA".equals(sesion.getEstado())) {
            throw new IllegalArgumentException("La sesión ya fue cerrada");
        }
        sesion.setHorarioId(dto.getHorarioId());
        sesion.setSeccionId(dto.getSeccionId());
        sesion.setCursoId(dto.getCursoId());
        sesion.setDocenteId(dto.getDocenteId());
        sesion.setFecha(dto.getFecha());
        sesion.setEstado("ABIERTA");
        sesion.setAbiertaPor(username);
        return toDto(sesionRepository.save(sesion));
    }

    public SesionAsistenciaDTO cerrarSesion(Long id, String username) {
        SesionAsistencia sesion = sesionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión de asistencia no encontrada"));
        if ("CERRADA".equals(sesion.getEstado())) return toDto(sesion);
        long registrados = asistenciaRepository.findByHorarioId(sesion.getHorarioId()).stream()
                .filter(item -> sesion.getFecha().equals(item.getFecha())).count();
        if (registrados == 0) throw new IllegalArgumentException("No se puede cerrar una sesión sin asistencias registradas");
        sesion.setEstado("CERRADA");
        sesion.setCerradaPor(username);
        sesion.setFechaCierre(LocalDateTime.now());
        return toDto(sesionRepository.save(sesion));
    }

    private void asegurarSesionAbierta(AsistenciaDTO dto, String username) {
        SesionAsistencia sesion = sesionRepository.findByHorarioIdAndFecha(dto.getHorarioId(), dto.getFecha())
                .orElseGet(() -> {
                    SesionAsistencia nueva = new SesionAsistencia();
                    nueva.setHorarioId(dto.getHorarioId());
                    nueva.setSeccionId(dto.getSeccionId());
                    nueva.setCursoId(dto.getCursoId());
                    nueva.setDocenteId(dto.getDocenteId());
                    nueva.setFecha(dto.getFecha());
                    nueva.setEstado("ABIERTA");
                    nueva.setAbiertaPor(username);
                    return sesionRepository.save(nueva);
                });
        if ("CERRADA".equals(sesion.getEstado())) {
            throw new IllegalArgumentException("La sesión de clase está cerrada y no admite cambios");
        }
    }

    private void validarRegistro(AsistenciaDTO dto) {
        String estado = dto.getEstado().trim().toUpperCase(Locale.ROOT);
        dto.setEstado(estado);
        if ("JUSTIFICADO".equals(estado) && (dto.getJustificacion() == null || dto.getJustificacion().isBlank())) {
            throw new IllegalArgumentException("Indica el motivo de la justificación");
        }
        if (!"JUSTIFICADO".equals(estado)) dto.setJustificacion(null);
    }

    private void copiar(AsistenciaDTO dto, Asistencia entity, String username) {
        entity.setFecha(dto.getFecha());
        entity.setEstado(dto.getEstado());
        entity.setEstudianteId(dto.getEstudianteId());
        entity.setHorarioId(dto.getHorarioId());
        entity.setDocenteId(dto.getDocenteId());
        entity.setSeccionId(dto.getSeccionId());
        entity.setCursoId(dto.getCursoId());
        entity.setTurno(dto.getTurno());
        entity.setJustificacion(dto.getJustificacion());
        entity.setRegistradoPor(username);
    }

    private SesionAsistenciaDTO toDto(SesionAsistencia entity) {
        SesionAsistenciaDTO dto = new SesionAsistenciaDTO();
        dto.setId(entity.getId());
        dto.setHorarioId(entity.getHorarioId());
        dto.setSeccionId(entity.getSeccionId());
        dto.setCursoId(entity.getCursoId());
        dto.setDocenteId(entity.getDocenteId());
        dto.setFecha(entity.getFecha());
        dto.setEstado(entity.getEstado());
        dto.setAbiertaPor(entity.getAbiertaPor());
        dto.setCerradaPor(entity.getCerradaPor());
        dto.setFechaApertura(entity.getFechaApertura());
        dto.setFechaCierre(entity.getFechaCierre());
        return dto;
    }
}
