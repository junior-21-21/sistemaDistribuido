package com.example.demo.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.dto.CrearUsuarioRequest;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.model.Usuario;
import com.example.demo.model.RolInstitucional;
import com.example.demo.repository.ApoderadoRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private static final java.util.Set<String> ESTADOS_PERMITIDOS = java.util.Set.of("ACTIVO", "PAUSADO", "BAJA");
    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String ESPECIALES = "!@#$%";

    private final UsuarioRepository repo;
    private final ApoderadoRepository apoderadoRepository;
    private final PasswordEncoder encoder;
    private final ModuloInstitucionService modulos;
    private final SecureRandom secureRandom = new SecureRandom();

    public UsuarioService(UsuarioRepository repo, ApoderadoRepository apoderadoRepository, PasswordEncoder encoder,
                          ModuloInstitucionService modulos) {
        this.repo = repo;
        this.apoderadoRepository = apoderadoRepository;
        this.encoder = encoder;
        this.modulos = modulos;
    }

    public List<UsuarioDTO> listarTodos() {
        return repo.findAll().stream()
                .map(UsuarioDTO::from)
                .toList();
    }

    public Page<UsuarioDTO> listarPaginado(String q, String rol, String estado, Pageable pageable) {
        return repo.buscar(normalizarFiltro(q), normalizarCodigo(rol), normalizarCodigo(estado), pageable)
                .map(UsuarioDTO::from);
    }

    @Transactional
    public UsuarioDTO crear(CrearUsuarioRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Set<String> roles = normalizarRoles(request.roles(), request.rol());
        String rolPrincipal = rolPrincipal(roles);

        if (repo.existsByUsername(username)) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado");
        }
        if (repo.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(encoder.encode(request.clave()));
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setEmail(email);
        usuario.setRol(rolPrincipal);
        usuario.setRoles(roles);
        usuario.setEstado("ACTIVO");
        usuario.setDebeCambiarClave(true);
        if (roles.contains("APODERADO")) {
            if (request.apoderadoId() == null) {
                throw new IllegalArgumentException("Selecciona el apoderado vinculado a la cuenta");
            }
            usuario.setApoderado(apoderadoRepository.findById(request.apoderadoId())
                    .orElseThrow(() -> new IllegalArgumentException("El apoderado indicado no existe")));
        } else if (request.apoderadoId() != null) {
            throw new IllegalArgumentException("Solo una cuenta APODERADO puede vincularse a un apoderado");
        }
        return UsuarioDTO.from(repo.save(usuario));
    }

    /** Cambia el puesto y los encargos de una cuenta existente (lo hace Dirección). */
    @Transactional
    public UsuarioDTO actualizarRoles(Long id, Set<String> nuevosRoles, String actorUsername) {
        Usuario usuario = buscarEntidad(id);
        Set<String> roles = normalizarRoles(nuevosRoles, null);
        if (usuario.getUsername().equalsIgnoreCase(actorUsername) && !roles.contains("DIRECTOR")) {
            throw new IllegalArgumentException("No puedes quitarte el rol de Dirección a ti mismo");
        }
        if (roles.contains("APODERADO") != usuario.getRoles().contains("APODERADO")) {
            throw new IllegalArgumentException("Las cuentas de apoderado no se convierten en cuentas de personal (ni al revés)");
        }
        usuario.setRoles(roles);
        usuario.setRol(rolPrincipal(roles));
        return UsuarioDTO.from(repo.save(usuario));
    }

    @Transactional
    public UsuarioDTO cambiarEstado(Long id, String nuevoEstado, String actorUsername) {
        Usuario usuario = buscarEntidad(id);
        String estado = nuevoEstado == null ? "" : nuevoEstado.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS_PERMITIDOS.contains(estado)) {
            throw new IllegalArgumentException("Estado inválido. Use ACTIVO, PAUSADO o BAJA");
        }
        if (usuario.getUsername().equalsIgnoreCase(actorUsername) && !"ACTIVO".equals(estado)) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta");
        }
        usuario.setEstado(estado);
        return UsuarioDTO.from(repo.save(usuario));
    }

    @Transactional
    public String restablecerClave(Long id) {
        Usuario usuario = buscarEntidad(id);
        String claveTemporal = generarClaveTemporal();
        usuario.setPassword(encoder.encode(claveTemporal));
        usuario.setDebeCambiarClave(true);
        repo.save(usuario);
        return claveTemporal;
    }

    @Transactional
    public void cambiarClavePropia(String username, String claveActual, String nuevaClave) {
        Usuario usuario = repo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!encoder.matches(claveActual, usuario.getPassword())) {
            throw new IllegalArgumentException("La clave actual no es correcta");
        }
        if (encoder.matches(nuevaClave, usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva clave debe ser diferente de la actual");
        }
        usuario.setPassword(encoder.encode(nuevaClave));
        usuario.setDebeCambiarClave(false);
        repo.save(usuario);
    }

    private Usuario buscarEntidad(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private String normalizarRol(String value) {
        String rol = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return RolInstitucional.from(rol).name();
    }

    private Set<String> normalizarRoles(Set<String> values, String legacyRole) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        if (values != null) values.forEach(value -> roles.add(normalizarRol(value)));
        if (roles.isEmpty() && legacyRole != null && !legacyRole.isBlank()) {
            roles.add(normalizarRol(legacyRole));
        }
        if (roles.isEmpty()) throw new IllegalArgumentException("Selecciona al menos un rol");
        validarCombinacion(roles);
        return roles;
    }

    /**
     * Reglas del colegio: cada cuenta tiene al menos un puesto (no solo encargos); los encargos solo
     * se dan a docentes o auxiliares; los roles opcionales exigen su módulo habilitado; el apoderado
     * no combina con roles del personal.
     */
    private void validarCombinacion(Set<String> roles) {
        Set<RolInstitucional> tipos = new java.util.LinkedHashSet<>();
        roles.forEach(rol -> tipos.add(RolInstitucional.from(rol)));
        boolean tienePuesto = tipos.stream().anyMatch(rol -> rol.tipo() != RolInstitucional.Tipo.ENCARGO);
        if (!tienePuesto) {
            throw new IllegalArgumentException("Un encargo se asigna a alguien con puesto de docente o auxiliar");
        }
        boolean tieneEncargo = tipos.stream().anyMatch(rol -> rol.tipo() == RolInstitucional.Tipo.ENCARGO);
        if (tieneEncargo && tipos.stream().noneMatch(RolInstitucional.PUEDEN_TENER_ENCARGO::contains)) {
            throw new IllegalArgumentException("Los encargos solo se asignan a docentes o auxiliares");
        }
        for (RolInstitucional rol : tipos) {
            if (rol.tipo() == RolInstitucional.Tipo.OPCIONAL && !modulos.habilitado(rol.modulo())) {
                throw new IllegalArgumentException("Primero habilita el módulo de " + nombreModulo(rol.modulo())
                        + " en la configuración del colegio");
            }
        }
        if (tipos.contains(RolInstitucional.APODERADO) && tipos.size() > 1) {
            throw new IllegalArgumentException("Una cuenta de apoderado no puede tener roles del personal");
        }
    }

    /** El rol principal es el primer puesto (no un encargo). */
    private String rolPrincipal(Set<String> roles) {
        return roles.stream()
                .filter(rol -> RolInstitucional.from(rol).tipo() != RolInstitucional.Tipo.ENCARGO)
                .findFirst().orElse(roles.iterator().next());
    }

    private static String nombreModulo(String modulo) {
        return switch (modulo) {
            case "PSICOLOGIA" -> "Psicología";
            case "ENFERMERIA" -> "Enfermería";
            default -> "Mantenimiento";
        };
    }

    private String generarClaveTemporal() {
        List<Character> caracteres = new ArrayList<>();
        caracteres.add(aleatorio(MAYUSCULAS));
        caracteres.add(aleatorio(MINUSCULAS));
        caracteres.add(aleatorio(NUMEROS));
        caracteres.add(aleatorio(ESPECIALES));
        String todos = MAYUSCULAS + MINUSCULAS + NUMEROS + ESPECIALES;
        while (caracteres.size() < 14) {
            caracteres.add(aleatorio(todos));
        }
        Collections.shuffle(caracteres, secureRandom);
        StringBuilder clave = new StringBuilder(caracteres.size());
        caracteres.forEach(clave::append);
        return clave.toString();
    }

    private char aleatorio(String caracteres) {
        return caracteres.charAt(secureRandom.nextInt(caracteres.length()));
    }

    private String normalizarFiltro(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarCodigo(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
    }
}
