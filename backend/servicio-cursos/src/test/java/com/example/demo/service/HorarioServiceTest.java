package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dto.HorarioDTO;
import com.example.demo.mapper.HorarioMapper;
import com.example.demo.model.Aula;
import com.example.demo.model.AsignacionAcademica;
import com.example.demo.model.Curso;
import com.example.demo.model.Seccion;
import com.example.demo.repository.AulaRepository;
import com.example.demo.repository.AsignacionAcademicaRepository;
import com.example.demo.repository.CursoRepository;
import com.example.demo.repository.DocenteRepository;
import com.example.demo.repository.HorarioRepository;
import com.example.demo.repository.SeccionRepository;

@ExtendWith(MockitoExtension.class)
class HorarioServiceTest {

    @Mock private HorarioRepository horarioRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private AulaRepository aulaRepository;
    @Mock private SeccionRepository seccionRepository;
    @Mock private AsignacionAcademicaRepository asignacionRepository;
    @Mock private HorarioMapper horarioMapper;
    private HorarioService service;

    @BeforeEach
    void setUp() {
        service = new HorarioService(horarioRepository, cursoRepository, docenteRepository,
                aulaRepository, seccionRepository, asignacionRepository, horarioMapper);
    }

    @Test
    void rechazaBloquesSuperpuestosSinBorrarElHorarioExistente() {
        HorarioDTO primero = horario("08:00", "09:30", 1L, 1L);
        HorarioDTO segundo = horario("09:00", "10:00", 2L, 2L);
        when(seccionRepository.existsById(1L)).thenReturn(true);
        Seccion seccion = new Seccion();
        seccion.setAnioEscolar(2026);
        seccion.setTurno("MANANA");
        seccion.setCapacidadMaxima(35);
        Aula aula = aula("MANANA", 35);
        AsignacionAcademica asignacion = new AsignacionAcademica();
        asignacion.setHorasSemanales(6);
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion));
        when(asignacionRepository.findByCursoIdAndDocenteIdAndSeccionIdAndAnioEscolarAndActivaTrue(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Optional.of(asignacion));
        when(cursoRepository.existsById(org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
        when(docenteRepository.existsById(org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
        when(aulaRepository.findById(org.mockito.ArgumentMatchers.anyLong())).thenReturn(Optional.of(aula));
        when(horarioRepository.findConflictosDocenteExcluyendoSeccion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(horarioRepository.findConflictosAulaExcluyendoSeccion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.reemplazarPorSeccion(1L, List.of(primero, segundo)));

        verify(horarioRepository, never()).deleteBySeccionId(1L);
    }

    @Test
    void rechazaAulaDeTurnoIncompatible() {
        HorarioDTO dto = horario("07:30", "08:15", 1L, 1L);
        Seccion seccion = new Seccion();
        seccion.setAnioEscolar(2026);
        seccion.setTurno("MANANA");
        seccion.setCapacidadMaxima(35);
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion));
        when(cursoRepository.existsById(1L)).thenReturn(true);
        when(docenteRepository.existsById(1L)).thenReturn(true);
        when(aulaRepository.findById(1L)).thenReturn(Optional.of(aula("TARDE", 35)));

        assertThrows(IllegalArgumentException.class, () -> service.guardar(dto));

        verify(horarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unaAsignacionDeUnPeriodoNoAceptaDosBloquesDeCuarentaYCincoMinutos() {
        HorarioDTO primero = horario("07:30", "08:15", 1L, 1L);
        HorarioDTO segundo = horario("08:15", "09:00", 1L, 1L);
        when(seccionRepository.existsById(1L)).thenReturn(true);
        Seccion seccion = new Seccion();
        seccion.setAnioEscolar(2026);
        seccion.setTurno("MANANA");
        seccion.setCapacidadMaxima(35);
        when(seccionRepository.findById(1L)).thenReturn(Optional.of(seccion));
        when(cursoRepository.existsById(1L)).thenReturn(true);
        when(docenteRepository.existsById(1L)).thenReturn(true);
        when(aulaRepository.findById(1L)).thenReturn(Optional.of(aula("MANANA", 35)));
        AsignacionAcademica asignacion = new AsignacionAcademica();
        asignacion.setHorasSemanales(1);
        Curso curso = new Curso();
        curso.setNombre("Matemática");
        asignacion.setCurso(curso);
        when(asignacionRepository.findByCursoIdAndDocenteIdAndSeccionIdAndAnioEscolarAndActivaTrue(
                1L, 1L, 1L, 2026)).thenReturn(Optional.of(asignacion));
        when(horarioRepository.findConflictosDocenteExcluyendoSeccion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(horarioRepository.findConflictosAulaExcluyendoSeccion(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.reemplazarPorSeccion(1L, List.of(primero, segundo)));

        verify(horarioRepository, never()).deleteBySeccionId(1L);
    }

    private HorarioDTO horario(String inicio, String fin, Long cursoId, Long docenteId) {
        HorarioDTO dto = new HorarioDTO();
        dto.setDiaSemana("LUNES");
        dto.setHoraInicio(LocalTime.parse(inicio));
        dto.setHoraFin(LocalTime.parse(fin));
        dto.setTurno("MANANA");
        dto.setAulaId(docenteId);
        dto.setSeccionId(1L);
        dto.setCursoId(cursoId);
        dto.setDocenteId(docenteId);
        return dto;
    }

    private Aula aula(String turno, int capacidad) {
        Aula aula = new Aula();
        aula.setActiva(true);
        aula.setTurnoDisponible(turno);
        aula.setCapacidadMaxima(capacidad);
        return aula;
    }
}
