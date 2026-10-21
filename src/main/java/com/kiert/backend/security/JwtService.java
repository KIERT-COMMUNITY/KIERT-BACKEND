// src/main/java/com/kiert/backend/security/JwtService.java
package com.kiert.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(JwtProperties jwtProperties) {
        String secret = jwtProperties.secret();
        this.expiracionMs = jwtProperties.expirationMs() != null
                ? jwtProperties.expirationMs()
                : 86400000L;

        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT Secret no configurado");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    String.format("Clave JWT insegura: %d bits. Se requieren mínimo 256 bits.", keyBytes.length * 8)
            );
        }

        this.clave = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT Service inicializado con HS256");
    }

    // ============================================================
    // GENERAR TOKEN BÁSICO (USUARIOS NORMALES)
    // ============================================================
    public String generarToken(Long usuarioId, String email) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(email)
                .claim("usuarioId", usuarioId)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    // ============================================================
    // GENERAR TOKEN CON ROL (ADMINS)
    // ============================================================
    public String generarTokenConRol(Long usuarioId, String email, String rol) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(email)
                .claim("usuarioId", usuarioId)
                .claim("role", rol)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    // ============================================================
    // EXTRAER CLAIMS
    // ============================================================
    public String extraerEmail(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public Long extraerUsuarioId(String token) {
        Object val = extraerTodosLosClaims(token).get("usuarioId");
        if (val == null) return null;
        if (val instanceof Integer i) return i.longValue();
        if (val instanceof Long l) return l;
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    public String extraerRol(String token) {
        Object val = extraerTodosLosClaims(token).get("role");
        return val != null ? val.toString() : null;
    }

    public Instant obtenerExpiracion(String token) {
        Date expiracion = extraerClaim(token, Claims::getExpiration);
        return expiracion.toInstant();
    }

    // ============================================================
    // VALIDACIÓN ESTRICTA
    // ============================================================
    public boolean esTokenValido(String token, String emailEsperado) {
        try {
            String email = extraerEmail(token);
            return email != null
                    && email.equalsIgnoreCase(emailEsperado)
                    && !estaExpirado(token);
        } catch (Exception e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    private boolean estaExpirado(String token) {
        try {
            Date expiracion = extraerClaim(token, Claims::getExpiration);
            return expiracion.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extraerTodosLosClaims(token);
        return resolver.apply(claims);
    }

    /**
     * Valida SOLO tokens firmados con HS256.
     * Si el token viene con otro alg (HS512, RS256, none...), lanza excepción.
     */
    private Claims extraerTodosLosClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Error al parsear token: {}", e.getMessage());
            throw e;
        }
    }
}