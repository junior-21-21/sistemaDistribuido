package com.example.demo.service;

import com.example.demo.dto.AsignacionAcademicaDTO;
import com.example.demo.model.AsignacionAcademica;
import com.example.demo.model.Curso;
import com.example.demo.model.Docente;
import com.example.demo.model.Seccion;
import com.example.demo.repository.AsignacionAcademicaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.SeccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AsignacionAcademicaService {
    private final AsignacionAcademicaRepository repository;
    private final CursoRepository cursoRepository;
    private final DocenteRepository docenteRepository;
    private final SeccionRepository seccionRepository;

    public AsignacionAcademicaService(AsignacionAcademicaRepository repository,
                                      CursoRepository cursoRepository,
                                      DocenteRepository docenteRepository,
                                      SeccionRepository seccionRepository) {
        this.repository = repository;
        this.cursoRepository = cursoRepository;
        this.docenteRepository = docenteRepository;
        this.seccionRepository = seccionRepository;
    }

    public List<AsignacionAcademicaDTO> listar(Long seccionId, Integer anioEscolar) {
        int anio = anioEscolar == null ? Year.now().getValue() : anioEscolar;
        List<AsignacionAcademica> asignaciones = seccionId == null
                ? repository.findByAnioEscolarAndActivaTrueOrderBySeccionIdAscCursoNombreAsc(anio)
                : repository.findBySeccionIdAndAnioEscolarAndActivaTrueOrderByCursoNombre(seccionId, anio);
        return asignaciones.stream().map(this::toDto).toList();
    }

    public List<AsignacionAcademicaDTO> listarMias(String username, Integer anioEscolar) {
        int anio = anioEscolar == null ? Year.now().getValue() : anioEscolar;
        return repository
                .findByDocenteUsernameIgnoreCaseAndAnioEscolarAndActivaTrueOrderBySeccionIdAscCursoNombreAsc(
                        username, anio)
                .stream().map(this::toDto).toList();
    }

    public boolean validarMia(String username, Long cursoId, Long docenteId, Long seccionId, Integer anioEscolar) {
        int anio = anioEscolar == null ? Year.now().getValue() : anioEscolar;
        return repository.existsByCursoIdAndDocenteIdAndSeccionIdAndAnioEscolarAndActivaTrueAndDocenteUsernameIgnoreCase(
                cursoId, docenteId, seccionId, anio, username);
    }

    public boolean validarSeccionMia(String username, Long seccionId, Integer anioEscolar) {
        int anio = anioEscolar == null ? Year.now().getValue() : anioEscolar;
        if (repository.existsBySeccionIdAndAnioEscolarAndActivaTrueAndDocenteUsernameIgnoreCase(
                seccionId, anio, username)) {
            return true;
        }
        // El docente tutor acompaña a su sección aunque no dicte un curso en ella.
        return docenteRepository.findByUsernameIgnoreCase(username)
                .map(docente -> seccionRepository.existsByIdAndDocenteTutorIdAndActivaTrue(seccionId, docente.getId()))
                .orElse(false);
    }

    @Transactional
    public AsignacionAcademicaDTO guardar(Long id, AsignacionAcademicaDTO dto) {
        Curso curso = cursoRepository.findById(dto.getCursoId())
                .orElseThrow(() -> new IllegalArgumentException("El curso indicado no existe"));
        Docente docente = docenteRepository.findById(dto.getDocenteId())
                .orElseThrow(() -> new IllegalArgumentException("El docente indicado no existe"));
        Seccion seccion = seccionRepository.findById(dto.getSeccionId())
                .orElseThrow(() -> new IllegalArgumentException("La sección indicada no existe"));
        if (!curso.getGradoId().equals(seccion.getGrado().getId())) {
            throw new IllegalArgumentException("El curso no pertenece al grado de la sección");
        }
        if (!"ACTIVO".equalsIgnoreCase(docente.getEstado())) {
            throw new IllegalArgumentException("Solo se puede asignar un docente activo");
        }
        long excluir = id == null ? -1L : id;
        if (repository.existsByCursoIdAndDocenteIdAndSeccionIdAndAnioEscolarAndIdNot(
                dto.getCursoId(), dto.getDocenteId(), dto.getSeccionId(), dto.getAnioEscolar(), excluir)) {
            throw new IllegalArgumentException("La asignación académica ya existe");
        }
        int contratadas = docente.getHorasSemanalesContratadas() == null ? 30 : docente.getHorasSemanalesContratadas();
        long asignadas = repository.sumarHorasDocente(docente.getId(), dto.getAnioEscolar(), id);
        if (asignadas + dto.getHorasSemanales() > contratadas) {
            throw new IllegalArgumentException("La carga excede las " + contratadas
                    + " horas semanales contratadas; quedan " + Math.max(0, contratadas - asignadas));
        }

        AsignacionAcademica entity = id == null ? new AsignacionAcademica()
                : repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));
        entity.setCurso(curso);
        entity.setDocente(docente);
        entity.setSeccion(seccion);
        entity.setHorasSemanales(dto.getHorasSemanales());
        entity.setAnioEscolar(dto.getAnioEscolar());
        entity.setActiva(dto.getActiva() == null || dto.getActiva());
        return toDto(repository.save(entity));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) throw new IllegalArgumentException("Asignación no encontrada");
        repository.deleteById(id);
    }

    private AsignacionAcademicaDTO toDto(AsignacionAcademica entity) {
        AsignacionAcademicaDTO dto = new AsignacionAcademicaDTO();
        dto.setId(entity.getId());
        dto.setCursoId(entity.getCurso().getId());
        dto.setCursoNombre(entity.getCurso().getNombre());
        dto.setDocenteId(entity.getDocente().getId());
        dto.setDocenteNombre(entity.getDocente().getNombreCompleto());
        dto.setSeccionId(entity.getSeccion().getId());
        dto.setSeccionNombre(entity.getSeccion().getGrado().getNombre() + " " + entity.getSeccion().getNombre());
        dto.setHorasSemanales(entity.getHorasSemanales());
        dto.setAnioEscolar(entity.getAnioEscolar());
        dto.setActiva(entity.getActiva());
        int contratadas = entity.getDocente().getHorasSemanalesContratadas() == null ? 30 : entity.getDocente().getHorasSemanalesContratadas();
        int asignadas = repository.sumarHorasDocente(entity.getDocente().getId(), entity.getAnioEscolar(), null).intValue();
        dto.setHorasContratadasDocente(contratadas);
        dto.setHorasAsignadasDocente(asignadas);
        dto.setHorasDisponiblesDocente(Math.max(0, contratadas - asignadas));
        return dto;
    }
}
