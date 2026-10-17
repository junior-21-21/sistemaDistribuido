package com.example.demo.horarios.controller;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.example.demo.horarios.domain.Lesson;
import com.example.demo.horarios.domain.ParametrosHorario;
import com.example.demo.horarios.domain.Room;
import com.example.demo.horarios.domain.Timeslot;
import com.example.demo.horarios.domain.Timetable;
import com.example.demo.horarios.repository.LessonRepository;
import com.example.demo.horarios.repository.ParametrosHorarioRepository;
import com.example.demo.horarios.repository.RoomRepository;
import com.example.demo.horarios.repository.TimeslotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horarios")
@PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
public class HorarioController {

    @Autowired
    private SolverManager<Timetable, String> solverManager;

    @Autowired
    private TimeslotRepository timeslotRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private ParametrosHorarioRepository parametrosRepository;

    private static final String JOB_ID = "HORARIO_ANUAL_DEFAULT";

    // 1. Configurar reglas ANTES de resolver
    @PutMapping("/parametros")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<String> guardarParametros(@RequestBody ParametrosHorario parametros) {
        if (parametros.getMaxClasesPorDiaDocente() < 1 || parametros.getMaxClasesPorDiaDocente() > 12) {
            return ResponseEntity.badRequest().body("El máximo de clases por día debe estar entre 1 y 12.");
        }
        // Hay un único registro de configuración; se actualiza si existe o se crea la primera vez.
        ParametrosHorario actual = parametrosActuales().orElseGet(ParametrosHorario::new);
        actual.setMaxClasesPorDiaDocente(parametros.getMaxClasesPorDiaDocente());
        parametrosRepository.save(actual);
        return ResponseEntity.ok("Parámetros actualizados exitosamente en PostgreSQL");
    }

    @GetMapping("/parametros")
    public ParametrosHorario getParametros() {
        return parametrosActuales().orElse(new ParametrosHorario(5));
    }

    // 2. EL MOTOR: Inicia el cálculo sacando los datos de PostgreSQL
    @PostMapping("/generar")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<String> generarHorario() {
        if (solverManager.getSolverStatus(JOB_ID) != SolverStatus.NOT_SOLVING) {
            return ResponseEntity.badRequest().body("Ya hay un horario generándose en este momento.");
        }

        // Sacamos TODA la información de PostgreSQL
        List<Timeslot> timeslots = timeslotRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<Lesson> lessons = lessonRepository.findAll();
        ParametrosHorario parametros = parametrosActuales().orElse(new ParametrosHorario(5));

        // Construimos el problema a resolver
        Timetable problemaBase = new Timetable(timeslots, rooms, lessons);
        problemaBase.setParametros(parametros);

        solverManager.solveBuilder()
                .withProblemId(JOB_ID)
                .withProblemFinder(id -> problemaBase)
                // Cada vez que el algoritmo encuentra una solución mejor,
                // guardamos las lecciones actualizadas (con aula y hora asignadas) en BD
                .withBestSolutionConsumer(mejorSolucion -> lessonRepository.saveAll(mejorSolucion.getLessonList()))
                .run();
        
        return ResponseEntity.ok("Generación iniciada con éxito. Procesando en segundo plano.");
    }

    // 3. CONTROL DE PÁNICO
    @PostMapping("/detener")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'SUBDIRECTOR')")
    public ResponseEntity<String> detenerHorario() {
        solverManager.terminateEarly(JOB_ID);
        return ResponseEntity.ok("Algoritmo detenido manualmente.");
    }

    // 4. MONITOREO
    @GetMapping("/estado")
    public SolverStatus getEstado() {
        return solverManager.getSolverStatus(JOB_ID);
    }

    // 5. RESULTADO FINAL DIRECTO DESDE BD
    @GetMapping("/resultado")
    public Timetable getHorario() {
        List<Timeslot> timeslots = timeslotRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<Lesson> lessons = lessonRepository.findAll();
        
        return new Timetable(timeslots, rooms, lessons);
    }

    private java.util.Optional<ParametrosHorario> parametrosActuales() {
        return parametrosRepository.findAll(org.springframework.data.domain.Sort.by("id")).stream().findFirst();
    }
}
