package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.ModuloInstitucion;
import com.example.demo.model.RolInstitucional;
import com.example.demo.repository.ModuloInstitucionRepository;
import com.example.demo.repository.UsuarioRepository;

/** Módulos opcionales del colegio. Solo se asignan sus roles si Dirección los habilita. */
@Service
@Transactional(readOnly = true)
public class ModuloInstitucionService {

    public static final String PSICOLOGIA = "PSICOLOGIA";
    public static final String ENFERMERIA = "ENFERMERIA";
    public static final String MANTENIMIENTO = "MANTENIMIENTO";
    public static final String PORTERIA = "PORTERIA";

    private final ModuloInstitucionRepository modulos;
    private final UsuarioRepository usuarios;

    public ModuloInstitucionService(ModuloInstitucionRepository modulos, UsuarioRepository usuarios) {
        this.modulos = modulos;
        this.usuarios = usuarios;
    }

    /** Estado de los módulos opcionales (los que no existen se consideran deshabilitados). */
    public Map<String, Boolean> estado() {
        Map<String, Boolean> estado = new LinkedHashMap<>();
        for (String codigo : new String[] { PSICOLOGIA, ENFERMERIA, MANTENIMIENTO, PORTERIA }) {
            estado.put(codigo, modulos.findById(codigo).map(ModuloInstitucion::isHabilitado).orElse(false));
        }
        return estado;
    }

    public boolean habilitado(String codigo) {
        return codigo == null || estado().getOrDefault(codigo, false);
    }

    @Transactional
    public Map<String, Boolean> cambiar(String codigo, boolean habilitar, String actor) {
        String clave = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        if (!estado().containsKey(clave)) {
            throw new IllegalArgumentException("Módulo desconocido");
        }
        if (!habilitar) {
            RolInstitucional rol = Arrays.stream(RolInstitucional.values())
                    .filter(item -> clave.equals(item.modulo())).findFirst().orElseThrow();
            long activos = usuarios.contarActivosConRol(rol.name());
            if (activos > 0) {
                throw new IllegalArgumentException("Hay " + activos + " cuenta(s) activa(s) con ese rol. "
                        + "Desactívalas o cámbiales el rol antes de deshabilitar el módulo.");
            }
        }
        ModuloInstitucion modulo = modulos.findById(clave).orElseGet(() -> {
            ModuloInstitucion nuevo = new ModuloInstitucion();
            nuevo.setCodigo(clave);
            return nuevo;
        });
        modulo.setHabilitado(habilitar);
        modulo.setActualizadoPor(actor == null ? "sistema" : actor);
        modulo.setActualizadoEn(LocalDateTime.now());
        modulos.save(modulo);
        return estado();
    }
}
