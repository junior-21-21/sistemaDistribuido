package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

/**
 * JwtUtil — Genera y valida tokens JWT para la Plataforma Académica.
 * 
 * El token contiene: username, rol, nombre completo.
 * Expiración: 1 hora (configurable).
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration:3600000}") long expirationMs
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un token JWT con los datos del usuario.
     */
    public String generateToken(String username, String rol, Set<String> roles, String nombreCompleto, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(username)
            .claim("rol", rol)
            .claim("roles", roles)
            .claim("nombre", nombreCompleto)
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }

    /**
     * Extrae el username del token.
     */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extrae el rol del token.
     */
    public String getRol(String token) {
        return getClaims(token).get("rol", String.class);
    }

    /**
     * Extrae el nombre completo del token.
     */
    public String getNombre(String token) {
        return getClaims(token).get("nombre", String.class);
    }

    /**
     * Valida que el token no esté expirado ni manipulado.
     */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
