package com.example.demo.service;

import com.example.demo.dto.ReporteDisciplinarioDTO;
import com.example.demo.mapper.ReporteDisciplinarioMapper;
import com.example.demo.model.ReporteDisciplinario;
import com.example.demo.repository.ReporteDisciplinarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReporteDisciplinarioService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("EMITIDO", "NOTIFICADO", "ARCHIVADO");

    @Autowired
    private ReporteDisciplinarioRepository repository;

    @Autowired
    private ReporteDisciplinarioMapper mapper;

    @Autowired
    private NotificacionClient notificacionClient;

    public List<ReporteDisciplinarioDTO> listarPorEstudiante(Long estudianteId) {
        return repository.findByEstudianteId(estudianteId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReporteDisciplinarioDTO> listar(Long seccionId) {
        List<ReporteDisciplinario> reportes = seccionId == null
                ? repository.findAllByOrderByFechaReporteDesc()
                : repository.findBySeccionIdOrderByFechaReporteDesc(seccionId);
        return reportes.stream().map(mapper::toDto).collect(Collectors.toList());
    }

    public ReporteDisciplinarioDTO guardar(ReporteDisciplinarioDTO dto, String registradoPor) {
        ReporteDisciplinario entidad = mapper.toEntity(dto);
        entidad.setId(null);
        // El protocolo se completa después, por TOE o Dirección.
        entidad.setCodigoSiseve(null);
        entidad.setFechaSiseve(null);
        entidad.setCitacionApoderado(null);
        entidad.setMedidasProteccion(null);
        entidad.setDerivacionExterna(null);
        entidad.setFechaDerivacion(null);
        entidad.setProtocoloActualizadoPor(null);
        entidad.setRegistradoPor(registradoPor);
        entidad = repository.save(entidad);
        notificacionClient.disciplina(entidad.getId(), entidad.getEstudianteId(),
                entidad.getTipo(), entidad.getDescripcion());
        return mapper.toDto(entidad);
    }

    public ReporteDisciplinarioDTO actualizarProtocolo(Long id, com.example.demo.dto.ProtocoloViolenciaRequest datos,
                                                       String actor) {
        ReporteDisciplinario entidad = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte disciplinario no encontrado"));
        if (!"VIOLENCIA_ESCOLAR".equals(entidad.getTipo()) && !"FALTA_GRAVE".equals(entidad.getTipo())) {
            throw new IllegalArgumentException("El protocolo se aplica a casos de violencia escolar o faltas graves");
        }
        if (datos.codigoSiseve() != null && !datos.codigoSiseve().isBlank()) {
            entidad.setCodigoSiseve(datos.codigoSiseve().trim());
            entidad.setFechaSiseve(datos.fechaSiseve() == null ? java.time.LocalDate.now() : datos.fechaSiseve());
        }
        if (datos.citacionApoderado() != null) {
            entidad.setCitacionApoderado(datos.citacionApoderado());
        }
        if (datos.medidasProteccion() != null && !datos.medidasProteccion().isBlank()) {
            entidad.setMedidasProteccion(datos.medidasProteccion().trim());
        }
        if (datos.derivacionExterna() != null && !datos.derivacionExterna().isBlank()) {
            entidad.setDerivacionExterna(datos.derivacionExterna());
            entidad.setFechaDerivacion(datos.fechaDerivacion() == null ? java.time.LocalDate.now() : datos.fechaDerivacion());
        }
        entidad.setProtocoloActualizadoPor(actor);
        return mapper.toDto(repository.save(entidad));
    }

    public ReporteDisciplinarioDTO actualizarEstado(Long id, String estado) {
        if (estado == null || !ESTADOS_VALIDOS.contains(estado)) {
            throw new IllegalArgumentException("Estado disciplinario no válido");
        }
        ReporteDisciplinario entidad = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte disciplinario no encontrado"));
        entidad.setEstado(estado);
        if ("NOTIFICADO".equals(estado)) {
            entidad.setApoderadoNotificado(true);
            entidad.setFechaNotificacion(LocalDateTime.now());
        }
        return mapper.toDto(repository.save(entidad));
    }
}
