package com.example.demo.service;

import com.example.demo.dto.EstudianteDTO;
import com.example.demo.dto.MatriculaRequest;
import com.example.demo.dto.RegistrarConsentimientoRequest;
import com.example.demo.mapper.EstudianteMapper;
import com.example.demo.model.ApoderadoEstudiante;
import com.example.demo.model.Estudiante;
import com.example.demo.repository.ApoderadoEstudianteRepository;
import com.example.demo.repository.ApoderadoRepository;
import com.example.demo.repository.EstudianteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.Locale;

@Service
@Transactional
public class EstudianteService {

    @Autowired
    private TrayectoriaService trayectoriaService;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private EstudianteMapper estudianteMapper;

    @Autowired
    private ApoderadoRepository apoderadoRepository;

    @Autowired
    private ApoderadoEstudianteRepository vinculoRepository;

    @Autowired
    private ConsentimientoDatosService consentimientoDatosService;

    public List<EstudianteDTO> listarTodos() {
        return estudianteRepository.findAll().stream()
                .map(estudianteMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<EstudianteDTO> listarPorSecciones(Set<Long> seccionIds) {
        if (seccionIds.isEmpty()) return List.of();
        return estudianteRepository.findAll((root, query, cb) -> cb.and(
                        root.get("seccionId").in(seccionIds), cb.equal(root.get("estado"), "ACTIVO"))).stream()
                .map(estudianteMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<EstudianteDTO> listarPaginado(String busqueda, Long gradoId, Collection<Long> seccionIds,
                                               String estado, Pageable pageable) {
        Specification<Estudiante> filtros = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String termino = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
            if (!termino.isBlank()) {
                String patron = "%" + termino + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("apellido")), patron),
                        cb.like(cb.lower(root.get("dni")), patron),
                        cb.like(cb.lower(root.get("email")), patron)));
            }
            if (gradoId != null) predicates.add(cb.equal(root.get("gradoId"), gradoId));
            if (seccionIds != null) {
                predicates.add(seccionIds.isEmpty() ? cb.disjunction() : root.get("seccionId").in(seccionIds));
            }
            if (estado != null && !estado.isBlank()) predicates.add(cb.equal(root.get("estado"), estado.toUpperCase(Locale.ROOT)));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return estudianteRepository.findAll(filtros, pageable).map(estudianteMapper::toDto);
    }

    public Optional<EstudianteDTO> buscarPorId(Long id) {
        return estudianteRepository.findById(id).map(estudianteMapper::toDto);
    }

    public EstudianteDTO guardar(EstudianteDTO estudianteDTO) {
        validarDuplicados(estudianteDTO, null);
        normalizar(estudianteDTO);
        Estudiante estudiante = estudianteMapper.toEntity(estudianteDTO);
        if (estudiante.getEstado() == null || estudiante.getEstado().isBlank()) {
            estudiante.setEstado("ACTIVO");
        }
        estudiante = estudianteRepository.save(estudiante);
        return estudianteMapper.toDto(estudiante);
    }

    public EstudianteDTO matricular(MatriculaRequest request, String actor) {
        var apoderado = apoderadoRepository.findById(request.apoderadoId())
                .orElseThrow(() -> new IllegalArgumentException("El apoderado seleccionado no existe"));
        EstudianteDTO dto = request.estudiante();
        normalizar(dto);

        Estudiante estudiante;
        if (dto.getId() == null) {
            validarDuplicados(dto, null);
            estudiante = estudianteMapper.toEntity(dto);
            estudiante.setEstado("ACTIVO");
        } else {
            estudiante = estudianteRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("El estudiante seleccionado no existe"));
            validarDuplicados(dto, estudiante.getId());
            estudianteMapper.updateEntityFromDto(dto, estudiante);
        }
        estudiante = estudianteRepository.save(estudiante);

        if (vinculoRepository.findByApoderadoIdAndEstudianteId(apoderado.getId(), estudiante.getId()).isEmpty()) {
            ApoderadoEstudiante vinculo = new ApoderadoEstudiante();
            vinculo.setApoderado(apoderado);
            vinculo.setEstudiante(estudiante);
            vinculo.setParentesco(request.parentesco().trim().toUpperCase(Locale.ROOT));
            vinculo.setPermisoRetiro(Boolean.TRUE.equals(request.permisoRetiro()));
            vinculoRepository.save(vinculo);
        }
        // Matrícula del año (gratuita y sin condicionamientos): tipo y procedencia, como en SIAGIE.
        trayectoriaService.registrarMatricula(estudiante, new TrayectoriaService.DatosMatricula(
                request.tipoMatricula(), request.ieProcedencia(), request.codigoModularProcedencia()), actor);
        if (request.consentimientos() != null) {
            for (var consentimiento : request.consentimientos()) {
                consentimientoDatosService.registrar(new RegistrarConsentimientoRequest(
                        estudiante.getId(), apoderado.getId(), consentimiento.finalidad(),
                        consentimiento.accion(), consentimiento.evidencia()), actor);
            }
        }
        return estudianteMapper.toDto(estudiante);
    }

    public Optional<EstudianteDTO> actualizar(Long id, EstudianteDTO detalles) {
        normalizar(detalles);
        validarDuplicados(detalles, id);
        return estudianteRepository.findById(id).map(estudiante -> {
            estudianteMapper.updateEntityFromDto(detalles, estudiante);
            return estudianteMapper.toDto(estudianteRepository.save(estudiante));
        });
    }

    public boolean eliminar(Long id) {
        return estudianteRepository.findById(id).map(estudiante -> {
            estudianteRepository.delete(estudiante);
            return true;
        }).orElse(false);
    }

    public long countTotal() {
        return estudianteRepository.count();
    }

    public long countPendientes() {
        return estudianteRepository.countByEstado("PENDIENTE");
    }

    private void normalizar(EstudianteDTO dto) {
        if (dto.getEmail() != null && dto.getEmail().isBlank()) dto.setEmail(null);
        if (dto.getTelefono() != null && dto.getTelefono().isBlank()) dto.setTelefono(null);
        if (dto.getEstado() == null || dto.getEstado().isBlank()) dto.setEstado("ACTIVO");
    }

    private void validarDuplicados(EstudianteDTO dto, Long idActual) {
        estudianteRepository.findByDni(dto.getDni())
                .filter(item -> !item.getId().equals(idActual))
                .ifPresent(item -> { throw new IllegalArgumentException("Ya existe un estudiante con ese DNI"); });
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            estudianteRepository.findByEmailIgnoreCase(dto.getEmail())
                    .filter(item -> !item.getId().equals(idActual))
                    .ifPresent(item -> { throw new IllegalArgumentException("Ya existe un estudiante con ese correo"); });
        }
    }
}
