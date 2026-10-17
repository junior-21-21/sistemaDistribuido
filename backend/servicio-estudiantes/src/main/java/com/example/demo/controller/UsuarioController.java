package com.example.demo.controller;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import com.example.demo.dto.CambiarEstadoUsuarioRequest;
import com.example.demo.dto.CrearUsuarioRequest;
import com.example.demo.dto.RestablecerClaveResponse;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('DIRECTOR')")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioDTO> listar() {
        return service.listarTodos();
    }

    @GetMapping("/page")
    public PageResponse<UsuarioDTO> listarPaginado(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) String rol,
                                           @RequestParam(required = false) String estado,
                                           @PageableDefault(size = 20, sort = "apellido") Pageable pageable) {
        return PageResponse.from(service.listarPaginado(q, rol, estado, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<UsuarioDTO> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioDTO creado = service.crear(request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + creado.id())).body(creado);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('DIRECTOR')")
    public UsuarioDTO cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoUsuarioRequest request,
            Principal principal) {
        return service.cambiarEstado(id, request.estado(), principal.getName());
    }

    /** Cambia el puesto y los encargos de la cuenta. Cuerpo: {"roles": ["AUXILIAR", "RESPONSABLE_CONVIVENCIA"]}. */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('DIRECTOR')")
    public UsuarioDTO actualizarRoles(@PathVariable Long id, @RequestBody java.util.Map<String, java.util.Set<String>> cuerpo,
                                      Principal principal) {
        return service.actualizarRoles(id, cuerpo.get("roles"), principal.getName());
    }

    @PostMapping("/{id}/restablecer-clave")
    @PreAuthorize("hasRole('DIRECTOR')")
    public RestablecerClaveResponse restablecerClave(@PathVariable Long id) {
        String clave = service.restablecerClave(id);
        return new RestablecerClaveResponse(clave, "Clave temporal generada correctamente");
    }
}
