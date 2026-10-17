package com.example.demo.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Duration;
import java.util.HashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.HorarioDTO;
import com.example.demo.mapper.HorarioMapper;
import com.example.demo.model.Aula;
import com.example.demo.model.Horario;
import com.example.demo.model.Seccion;
import com.example.demo.repository.AulaRepository;
import com.example.demo.repository.AsignacionAcademicaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.HorarioRepository;
import com.example.demo.repository.SeccionRepository;

@Service
@Transactional(readOnly = true)
public class HorarioService {

    private static final long MINUTOS_PERIODO_PEDAGOGICO = 45L;
    private static final Set<String> DIAS_VALIDOS = Set.of(
            "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO");
    private static final Set<String> TURNOS_VALIDOS = Set.of("MANANA", "TARDE");

    private final HorarioRepository horarioRepository;
    private final CursoRepository cursoRepository;
    private final DocenteRepository docenteRepository;
    private final AulaRepository aulaRepository;
    private final SeccionRepository seccionRepository;
    private final AsignacionAcademicaRepository asignacionRepository;
    private final HorarioMapper horarioMapper;

    public HorarioService(
            HorarioRepository horarioRepository,
            CursoRepository cursoRepository,
            DocenteRepository docenteRepository,
            AulaRepository aulaRepository,
            SeccionRepository seccionRepository,
            AsignacionAcademicaRepository asignacionRepository,
            HorarioMapper horarioMapper) {
        this.horarioRepository = horarioRepository;
        this.cursoRepository = cursoRepository;
        this.docenteRepository = docenteRepository;
        this.aulaRepository = aulaRepository;
        this.seccionRepository = seccionRepository;
        this.asignacionRepository = asignacionRepository;
        this.horarioMapper = horarioMapper;
    }

    public List<HorarioDTO> listarPorSeccion(Long seccionId) {
        return horarioRepository.findBySeccionId(seccionId).stream()
                .map(horarioMapper::toDto)
                .toList();
    }

    public List<HorarioDTO> listarPorDocente(Long docenteId) {
        return horarioRepository.findByDocenteId(docenteId).stream()
                .map(horarioMapper::toDto)
                .toList();
    }

    public List<HorarioDTO> listarMios(String username) {
        return horarioRepository.findByDocenteUsernameIgnoreCase(username).stream()
                .map(horarioMapper::toDto)
                .toList();
    }

    @Transactional
    public HorarioDTO guardar(HorarioDTO dto) {
        validarReferencias(dto);
        validarCargaSemanal(dto, null);
        if (!horarioRepository.findConflictosSeccion(
                dto.getSeccionId(), dto.getDiaSemana(), dto.getHoraInicio(), dto.getHoraFin()).isEmpty()) {
            throw new IllegalArgumentException("La sección ya tiene una clase asignada en ese horario");
        }
        validarConflictosExternos(dto, null);
        return horarioMapper.toDto(horarioRepository.save(crearEntidad(dto)));
    }

    @Transactional
    public List<HorarioDTO> reemplazarPorSeccion(Long seccionId, List<HorarioDTO> horarios) {
        if (!seccionRepository.existsById(seccionId)) {
            throw new IllegalArgumentException("La sección indicada no existe");
        }

        for (HorarioDTO dto : horarios) {
            if (!seccionId.equals(dto.getSeccionId())) {
                throw new IllegalArgumentException("Todos los bloques deben pertenecer a la sección solicitada");
            }
            validarReferencias(dto);
            validarConflictosExternos(dto, seccionId);
        }
        validarCrucesDentroDelLote(horarios);
        validarCargaSemanalLote(seccionId, horarios);

        horarioRepository.deleteBySeccionId(seccionId);
        List<Horario> guardados = horarioRepository.saveAll(horarios.stream()
                .map(this::crearEntidad)
                .toList());
        return guardados.stream().map(horarioMapper::toDto).toList();
    }

    /**
     * Reemplaza los horarios de varias secciones en una sola transacción. Permite publicar un
     * turno completo regenerado: los cruces se validan contra el resto del colegio y entre las
     * propias secciones del lote, sin chocar con los horarios antiguos que se van a reemplazar.
     */
    @Transactional
    public Map<Long, List<HorarioDTO>> reemplazarVariasSecciones(Map<Long, List<HorarioDTO>> porSeccion) {
        if (porSeccion.isEmpty()) {
            throw new IllegalArgumentException("No se enviaron secciones para guardar");
        }
        porSeccion.forEach((seccionId, horarios) -> {
            if (!seccionRepository.existsById(seccionId)) {
                throw new IllegalArgumentException("La sección " + seccionId + " no existe");
            }
            for (HorarioDTO dto : horarios) {
                if (!seccionId.equals(dto.getSeccionId())) {
                    throw new IllegalArgumentException("Todos los bloques deben pertenecer a su sección");
                }
                validarReferencias(dto);
            }
            validarCrucesDentroDelLote(horarios);
            validarCargaSemanalLote(seccionId, horarios);
        });

        List<HorarioDTO> todos = porSeccion.values().stream().flatMap(List::stream).toList();
        validarCrucesEntreSecciones(todos);

        porSeccion.keySet().forEach(horarioRepository::deleteBySeccionId);
        horarioRepository.flush();
        for (HorarioDTO dto : todos) {
            validarConflictosExternos(dto, null);
        }

        Map<Long, List<HorarioDTO>> resultado = new java.util.LinkedHashMap<>();
        porSeccion.forEach((seccionId, horarios) -> resultado.put(seccionId, horarioRepository
                .saveAll(horarios.stream().map(this::crearEntidad).toList())
                .stream().map(horarioMapper::toDto).toList()));
        return resultado;
    }

    private void validarCrucesEntreSecciones(List<HorarioDTO> horarios) {
        for (int i = 0; i < horarios.size(); i++) {
            HorarioDTO a = horarios.get(i);
            for (int j = i + 1; j < horarios.size(); j++) {
                HorarioDTO b = horarios.get(j);
                boolean mismoMomento = a.getDiaSemana().equals(b.getDiaSemana())
                        && a.getHoraInicio().isBefore(b.getHoraFin()) && a.getHoraFin().isAfter(b.getHoraInicio());
                if (!mismoMomento || a.getSeccionId().equals(b.getSeccionId())) continue;
                if (a.getDocenteId().equals(b.getDocenteId())) {
                    throw new IllegalArgumentException("Un docente quedó en dos secciones a la vez el "
                            + a.getDiaSemana().toLowerCase() + " a las " + a.getHoraInicio());
                }
                if (a.getAulaId().equals(b.getAulaId())) {
                    throw new IllegalArgumentException("Un aula quedó asignada a dos secciones el "
                            + a.getDiaSemana().toLowerCase() + " a las " + a.getHoraInicio());
                }
            }
        }
    }

    @Transactional
    public boolean eliminar(Long id) {
        if (!horarioRepository.existsById(id)) return false;
        horarioRepository.deleteById(id);
        return true;
    }

    private void validarReferencias(HorarioDTO dto) {
        if (dto.getHoraInicio() == null || dto.getHoraFin() == null
                || !dto.getHoraFin().isAfter(dto.getHoraInicio())) {
            throw new IllegalArgumentException("El rango horario es inválido");
        }
        if (!DIAS_VALIDOS.contains(dto.getDiaSemana())) {
            throw new IllegalArgumentException("El día de la semana es inválido");
        }
        if (!TURNOS_VALIDOS.contains(dto.getTurno())) {
            throw new IllegalArgumentException("El turno es inválido");
        }
        Seccion seccion = seccionRepository.findById(dto.getSeccionId())
                .orElseThrow(() -> new IllegalArgumentException("La sección indicada no existe"));
        if (!cursoRepository.existsById(dto.getCursoId())) {
            throw new IllegalArgumentException("El curso indicado no existe");
        }
        if (!docenteRepository.existsById(dto.getDocenteId())) {
            throw new IllegalArgumentException("El docente indicado no existe");
        }
        Aula aula = aulaRepository.findById(dto.getAulaId())
                .orElseThrow(() -> new IllegalArgumentException("El aula indicada no existe"));
        if (!dto.getTurno().equalsIgnoreCase(seccion.getTurno())) {
            throw new IllegalArgumentException("El turno del bloque no coincide con el turno de la sección");
        }
        if (!Boolean.TRUE.equals(aula.getActiva())) {
            throw new IllegalArgumentException("El aula indicada está inactiva");
        }
        String turnoAula = aula.getTurnoDisponible();
        if (!"AMBOS".equalsIgnoreCase(turnoAula) && !dto.getTurno().equalsIgnoreCase(turnoAula)) {
            throw new IllegalArgumentException("El aula no está disponible para el turno de la sección");
        }
        if (aula.getCapacidadMaxima() < seccion.getCapacidadMaxima()) {
            throw new IllegalArgumentException("La capacidad del aula es menor que la capacidad de la sección");
        }
        int anio = seccion.getAnioEscolar();
        if (asignacionRepository.findByCursoIdAndDocenteIdAndSeccionIdAndAnioEscolarAndActivaTrue(
                dto.getCursoId(), dto.getDocenteId(), dto.getSeccionId(), anio).isEmpty()) {
            throw new IllegalArgumentException(
                    "El curso y docente no tienen una asignación académica activa para esta sección");
        }
    }

    private void validarCargaSemanal(HorarioDTO dto, Long seccionExcluida) {
        int anio = seccionRepository.findById(dto.getSeccionId()).orElseThrow().getAnioEscolar();
        var asignacion = asignacionRepository
                .findByCursoIdAndDocenteIdAndSeccionIdAndAnioEscolarAndActivaTrue(
                        dto.getCursoId(), dto.getDocenteId(), dto.getSeccionId(), anio)
                .orElseThrow(() -> new IllegalArgumentException("No existe una carga académica activa"));
        var existentes = horarioRepository.findBySeccionId(dto.getSeccionId()).stream()
                .filter(h -> seccionExcluida == null)
                .filter(h -> h.getCurso().getId().equals(dto.getCursoId())
                        && h.getDocente().getId().equals(dto.getDocenteId()))
                .toList();
        long minutosExistentes = existentes.stream()
                .mapToLong(h -> Duration.between(h.getHoraInicio(), h.getHoraFin()).toMinutes())
                .sum();
        long nuevos = Duration.between(dto.getHoraInicio(), dto.getHoraFin()).toMinutes();
        if (excedeCarga(existentes.size() + 1, minutosExistentes + nuevos, asignacion.getHorasSemanales())) {
            throw new IllegalArgumentException("El bloque excede la carga semanal de "
                    + asignacion.getHorasSemanales() + " períodos pedagógicos para el curso y docente");
        }
    }

    private void validarCargaSemanalLote(Long seccionId, List<HorarioDTO> horarios) {
        int anio = seccionRepository.findById(seccionId).orElseThrow().getAnioEscolar();
        Map<String, Long> minutosPorCarga = new HashMap<>();
        Map<String, Integer> bloquesPorCarga = new HashMap<>();
        for (HorarioDTO dto : horarios) {
            String clave = dto.getCursoId() + "|" + dto.getDocenteId();
            minutosPorCarga.merge(clave,
                    Duration.between(dto.getHoraInicio(), dto.getHoraFin()).toMinutes(), Long::sum);
            bloquesPorCarga.merge(clave, 1, Integer::sum);
        }
        minutosPorCarga.forEach((clave, minutos) -> {
            String[] ids = clave.split("\\|");
            Long cursoId = Long.valueOf(ids[0]);
            Long docenteId = Long.valueOf(ids[1]);
            var asignacion = asignacionRepository
                    .findByCursoIdAndDocenteIdAndSeccionIdAndAnioEscolarAndActivaTrue(
                            cursoId, docenteId, seccionId, anio)
                    .orElseThrow(() -> new IllegalArgumentException("La propuesta contiene una carga no autorizada"));
            if (excedeCarga(bloquesPorCarga.get(clave), minutos, asignacion.getHorasSemanales())) {
                throw new IllegalArgumentException("La propuesta excede la carga semanal de "
                        + asignacion.getHorasSemanales() + " períodos pedagógicos para "
                        + asignacion.getCurso().getNombre());
            }
        });
    }

    /**
     * La carga semanal se mide en períodos. El colegio puede configurar períodos de 40, 45 o
     * 50 minutos, así que se acepta si no se supera ni el número de bloques ni el equivalente
     * en minutos de 45 (bloques dobles guardados como uno solo).
     */
    private static boolean excedeCarga(int bloques, long minutos, int periodosSemanales) {
        return bloques > periodosSemanales && minutos > periodosSemanales * MINUTOS_PERIODO_PEDAGOGICO;
    }

    private void validarConflictosExternos(HorarioDTO dto, Long seccionExcluida) {
        boolean docenteOcupado = seccionExcluida == null
                ? !horarioRepository.findConflictosDocente(dto.getDocenteId(), dto.getDiaSemana(),
                        dto.getHoraInicio(), dto.getHoraFin()).isEmpty()
                : !horarioRepository.findConflictosDocenteExcluyendoSeccion(dto.getDocenteId(), seccionExcluida,
                        dto.getDiaSemana(), dto.getHoraInicio(), dto.getHoraFin()).isEmpty();
        if (docenteOcupado) {
            throw new IllegalArgumentException("El docente ya tiene una clase asignada en otra sección el "
                    + dto.getDiaSemana().toLowerCase() + " a las " + dto.getHoraInicio()
                    + ". Si regeneraste varias secciones, usa “Guardar todo”.");
        }

        boolean aulaOcupada = seccionExcluida == null
                ? !horarioRepository.findConflictosAula(dto.getAulaId(), dto.getDiaSemana(),
                        dto.getHoraInicio(), dto.getHoraFin()).isEmpty()
                : !horarioRepository.findConflictosAulaExcluyendoSeccion(dto.getAulaId(), seccionExcluida,
                        dto.getDiaSemana(), dto.getHoraInicio(), dto.getHoraFin()).isEmpty();
        if (aulaOcupada) {
            throw new IllegalArgumentException("El aula ya está ocupada el "
                    + dto.getDiaSemana().toLowerCase() + " a las " + dto.getHoraInicio());
        }
    }

    private void validarCrucesDentroDelLote(List<HorarioDTO> horarios) {
        for (int i = 0; i < horarios.size(); i++) {
            HorarioDTO actual = horarios.get(i);
            for (int j = i + 1; j < horarios.size(); j++) {
                HorarioDTO siguiente = horarios.get(j);
                boolean mismoDia = actual.getDiaSemana().equals(siguiente.getDiaSemana());
                boolean seSuperponen = actual.getHoraInicio().isBefore(siguiente.getHoraFin())
                        && actual.getHoraFin().isAfter(siguiente.getHoraInicio());
                if (mismoDia && seSuperponen) {
                    throw new IllegalArgumentException("La propuesta contiene bloques superpuestos para la sección");
                }
            }
        }
    }

    private Horario crearEntidad(HorarioDTO dto) {
        Horario horario = horarioMapper.toEntity(dto);
        horario.setId(null);
        horario.setCurso(cursoRepository.getReferenceById(dto.getCursoId()));
        horario.setDocente(docenteRepository.getReferenceById(dto.getDocenteId()));
        return horario;
    }
}
