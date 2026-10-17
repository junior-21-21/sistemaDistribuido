package com.example.demo.model;

import java.util.Locale;
import java.util.Set;

/**
 * Roles del colegio público. Hay tres tipos:
 * <ul>
 *   <li>BASE: el puesto real de la persona (Dirección, subdirección, docente, auxiliar, secretaría, apoderado).</li>
 *   <li>OPCIONAL: personal que solo algunos colegios tienen; Dirección habilita su módulo.</li>
 *   <li>ENCARGO: función que Dirección asigna a un docente o auxiliar que ya trabaja en el colegio.</li>
 * </ul>
 */
public enum RolInstitucional {
    DIRECTOR(Tipo.BASE, null),
    /** Subdirección: lo pedagógico de su nivel (carga, horarios, notas, monitoreo). No gestiona cuentas ni firma nóminas. */
    SUBDIRECTOR(Tipo.BASE, null),
    DOCENTE(Tipo.BASE, null),
    AUXILIAR(Tipo.BASE, null),
    SECRETARIA(Tipo.BASE, null),
    APODERADO(Tipo.BASE, null),
    PSICOLOGO(Tipo.OPCIONAL, "PSICOLOGIA"),
    ENFERMERA(Tipo.OPCIONAL, "ENFERMERIA"),
    MANTENIMIENTO(Tipo.OPCIONAL, "MANTENIMIENTO"),
    /** Personal de servicio y guardianía: control de ingreso y de las papeletas de salida en la puerta. */
    PORTERO(Tipo.OPCIONAL, "PORTERIA"),
    /** Coordinación de Tutoría y Orientación Educativa (TOE) dentro del Comité de Gestión del Bienestar. */
    RESPONSABLE_CONVIVENCIA(Tipo.ENCARGO, null),
    PRIMEROS_AUXILIOS(Tipo.ENCARGO, null);

    public enum Tipo { BASE, OPCIONAL, ENCARGO }

    /** Puestos a los que se puede dar un encargo. */
    public static final Set<RolInstitucional> PUEDEN_TENER_ENCARGO = Set.of(DOCENTE, AUXILIAR);

    private final Tipo tipo;
    private final String modulo;

    RolInstitucional(Tipo tipo, String modulo) {
        this.tipo = tipo;
        this.modulo = modulo;
    }

    public Tipo tipo() {
        return tipo;
    }

    /** Módulo que debe estar habilitado para asignar este rol (solo roles opcionales). */
    public String modulo() {
        return modulo;
    }

    public static RolInstitucional from(String value) {
        try {
            return valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Rol institucional inválido");
        }
    }
}
