package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

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
        @Value("${jwt.secret:PAD_SecretKey_2026_HuamangaAyacucho_MuySegura!}") String secret,
        @Value("${jwt.expiration:3600000}") long expirationMs
    ) {
        // Asegura que la clave tenga al menos 256 bits para HS256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un token JWT con los datos del usuario.
     */
    public String generateToken(String username, String rol, String nombreCompleto) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(username)
            .claim("rol", rol)
            .claim("nombre", nombreCompleto)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
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
