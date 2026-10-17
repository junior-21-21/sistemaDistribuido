package com.example.demo.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class HorarioDTO {
    private Long id;
    @NotBlank
    @Pattern(regexp = "LUNES|MARTES|MIERCOLES|JUEVES|VIERNES|SABADO")
    private String diaSemana;
    @NotNull private LocalTime horaInicio;
    @NotNull private LocalTime horaFin;
    @NotBlank @Pattern(regexp = "MANANA|TARDE") private String turno;
    @NotNull @Positive private Long aulaId;
    @NotNull @Positive private Long seccionId;
    @NotNull @Positive private Long cursoId;
    private String cursoNombre;
    @NotNull @Positive private Long docenteId;
    private String docenteNombre;

    public HorarioDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }
    public Long getAulaId() { return aulaId; }
    public void setAulaId(Long aulaId) { this.aulaId = aulaId; }
    public Long getSeccionId() { return seccionId; }
    public void setSeccionId(Long seccionId) { this.seccionId = seccionId; }
    public Long getCursoId() { return cursoId; }
    public void setCursoId(Long cursoId) { this.cursoId = cursoId; }
    public String getCursoNombre() { return cursoNombre; }
    public void setCursoNombre(String cursoNombre) { this.cursoNombre = cursoNombre; }
    public Long getDocenteId() { return docenteId; }
    public void setDocenteId(Long docenteId) { this.docenteId = docenteId; }
    public String getDocenteNombre() { return docenteNombre; }
    public void setDocenteNombre(String docenteNombre) { this.docenteNombre = docenteNombre; }

    @AssertTrue(message = "La hora de fin debe ser posterior a la hora de inicio")
    public boolean isRangoHorarioValido() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}
