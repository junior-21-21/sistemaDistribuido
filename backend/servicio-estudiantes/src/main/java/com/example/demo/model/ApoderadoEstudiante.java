package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "apoderado_estudiante")
public class ApoderadoEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apoderado_id", nullable = false)
    private Apoderado apoderado;

    @Column(nullable = false)
    private String parentesco; // Ej: PADRE, MADRE, ABUELO

    @Column(name = "permiso_retiro", nullable = false)
    private Boolean permisoRetiro = false;

    public ApoderadoEstudiante() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Estudiante getEstudiante() { return estudiante; }
    public void setEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }
    public Apoderado getApoderado() { return apoderado; }
    public void setApoderado(Apoderado apoderado) { this.apoderado = apoderado; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public Boolean getPermisoRetiro() { return permisoRetiro; }
    public void setPermisoRetiro(Boolean permisoRetiro) { this.permisoRetiro = permisoRetiro; }
}
