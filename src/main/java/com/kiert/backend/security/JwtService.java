// src/main/java/com/kiert/backend/security/JwtService.java
package com.kiert.backend.security;

import io.jsonwebtoken.Claims;
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
        this.expiracionMs = jwtProperties.expirationMs() != null ? jwtProperties.expirationMs() : 86400000L;

        if (secret == null || secret.isEmpty()) {
            log.error("JWT Secret no está configurado");
            throw new IllegalStateException("JWT Secret no configurado");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 32) {
            log.error("Clave JWT tiene {} bits. Mínimo 256 bits requeridos.", keyBytes.length * 8);
            throw new IllegalStateException(
                    String.format("Clave JWT insegura: %d bits. Se requieren mínimo 256 bits.", keyBytes.length * 8)
            );
        }

        this.clave = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT Service inicializado con HS256");
        log.info("Clave JWT: {} bits ({} caracteres)", keyBytes.length * 8, secret.length());
    }

    // ============================================================
    // GENERAR TOKEN
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
    // EXTRAER DATOS
    // ============================================================
    public String extraerEmail(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public Long extraerUsuarioId(String token) {
        return extraerTodosLosClaims(token).get("usuarioId", Long.class);
    }

    /**
     * NUEVO: Devuelve la fecha de expiración del token.
     * Útil para el TTL de la blacklist: cuando el token expire,
     * ya no hace falta mantenerlo en la lista negra.
     */
    public Instant obtenerExpiracion(String token) {
        Date expiracion = extraerClaim(token, Claims::getExpiration);
        return expiracion.toInstant();
    }

    // ============================================================
    // VALIDAR
    // ============================================================
    public boolean esTokenValido(String token, String emailEsperado) {
        try {
            String email = extraerEmail(token);
            boolean valido = email.equals(emailEsperado) && !estaExpirado(token);
            log.debug("Token válido: {}", valido);
            return valido;
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
            log.error("Error al verificar expiración: {}", e.getMessage());
            return true;
        }
    }

    private <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extraerTodosLosClaims(token);
        return resolver.apply(claims);
    }

    /**
     * Valida SOLO tokens firmados con HS256.
     * Si el token viene con otro alg (HS512, RS256, none...), lanza excepción
     * controlada que el filtro convertirá en 401 (no en 500).
     */
    private Claims extraerTodosLosClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Firma del token inválida: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error al parsear token: {}", e.getMessage());
            throw e;
        }
    }
}