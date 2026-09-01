package com.example.demo.controller;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * AuthController — Maneja login/logout con JWT.
 * 
 * POST /api/auth/login → valida credenciales → retorna JWT + datos del usuario
 * GET  /api/auth/me    → retorna datos del usuario autenticado (requiere JWT)
 * POST /api/auth/logout → limpia contexto (informativo, JWT es stateless)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * POST /api/auth/login
     * Body: { "username": "director", "password": "admin123" }
     * Retorna: { "token": "eyJ...", "username", "nombre", "rol", "email" }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            if (username == null || password == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuario y contraseña son requeridos"));
            }

            // Autenticar contra la BD (BCrypt)
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username.trim(), password)
            );

            Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username.trim());
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no encontrado"));
            }

            Usuario usuario = usuarioOpt.get();
            String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();

            // Generar JWT
            String token = jwtUtil.generateToken(
                usuario.getUsername(),
                usuario.getRol(),
                nombreCompleto
            );

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", usuario.getUsername());
            response.put("nombre", nombreCompleto);
            response.put("rol", usuario.getRol());
            response.put("email", usuario.getEmail());
            response.put("estado", usuario.getEstado());
            response.put("message", "Login exitoso");

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "error", "Credenciales incorrectas",
                    "message", "Usuario o contraseña inválidos"
                ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    /**
     * POST /api/auth/logout
     * Con JWT stateless, el logout se maneja en el frontend (borrar token).
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
    }

    /**
     * GET /api/auth/me — Retorna el usuario autenticado (requiere JWT válido)
     */
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "No hay sesión activa"));
        }

        String username = auth.getName();
        return usuarioRepository.findByUsername(username)
            .map(u -> {
                Map<String, Object> response = new HashMap<>();
                response.put("username", u.getUsername());
                response.put("nombre", u.getNombre() + " " + u.getApellido());
                response.put("rol", u.getRol());
                response.put("email", u.getEmail());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
