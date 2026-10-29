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

    private static final String ROL_POR_DEFECTO = "USER";
    private static final long EXPIRACION_POR_DEFECTO_MS = 86400000L;

    public JwtService(JwtProperties jwtProperties) {
        String secret = jwtProperties.secret();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT Secret no configurado. Define 'kiert.jwt.secret' en application.yml"
            );
        }

        this.expiracionMs = jwtProperties.expirationMs() != null
                ? jwtProperties.expirationMs()
                : EXPIRACION_POR_DEFECTO_MS;

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    String.format(
                            "Clave JWT insegura: %d bits. Se requieren minimo 256 bits (32 bytes).",
                            keyBytes.length * 8
                    )
            );
        }

        this.clave = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT Service inicializado con HS256 (expiracion: {} ms)", expiracionMs);
    }

    // ============================================================
    // GENERAR TOKEN
    // ============================================================
    public String generarToken(Long usuarioId, String email, int tokenVersion) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(email)
                .claim("usuarioId", usuarioId)
                .claim("role", ROL_POR_DEFECTO)
                .claim("tokenVersion", tokenVersion)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    public String generarTokenConRol(Long usuarioId, String email, String rol, int tokenVersion) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        String rolFinal = (rol != null && !rol.isBlank()) ? rol : ROL_POR_DEFECTO;

        return Jwts.builder()
                .subject(email)
                .claim("usuarioId", usuarioId)
                .claim("role", rolFinal)
                .claim("tokenVersion", tokenVersion)
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

    /**
     * NUEVO: Extrae el "subject" del JWT.
     * Util como fallback cuando el token no tiene claim "email" (p. ej. tokens de admin).
     */
    public String extraerSubject(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public Long extraerUsuarioId(String token) {
        Object val = extraerTodosLosClaims(token).get("usuarioId");
        if (val == null) return null;
        if (val instanceof Integer i) return i.longValue();
        if (val instanceof Long l) return l;
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public String extraerRol(String token) {
        Object val = extraerTodosLosClaims(token).get("role");
        if (val == null) return ROL_POR_DEFECTO;
        String rol = val.toString().trim();
        return rol.isEmpty() ? ROL_POR_DEFECTO : rol;
    }

    public Integer extraerTokenVersion(String token) {
        Object val = extraerTodosLosClaims(token).get("tokenVersion");
        if (val == null) return 0;
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    public Instant obtenerExpiracion(String token) {
        Date expiracion = extraerClaim(token, Claims::getExpiration);
        return expiracion.toInstant();
    }

    // ============================================================
    // VALIDACION
    // ============================================================
    public boolean esTokenValido(String token, String emailEsperado) {
        try {
            String email = extraerEmail(token);
            return email != null
                    && email.equalsIgnoreCase(emailEsperado)
                    && !estaExpirado(token);
        } catch (Exception e) {
            log.warn("Token invalido: {}", e.getMessage());
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