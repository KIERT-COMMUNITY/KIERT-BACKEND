package com.kiert.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(JwtProperties jwtProperties) {
        // ✅ Obtener secret y expiration del record
        String secret = jwtProperties.secret();
        this.expiracionMs = jwtProperties.expirationMs() != null ? jwtProperties.expirationMs() : 86400000L;

        // ✅ Validar que la clave no sea nula
        if (secret == null || secret.isEmpty()) {
            log.error("❌ JWT Secret no está configurado");
            throw new IllegalStateException("JWT Secret no configurado. Verifica application.yml o Vault.");
        }

        // ✅ Convertir a bytes
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // ✅ Validar tamaño mínimo de 256 bits (32 bytes)
        if (keyBytes.length < 32) {
            log.error("❌ Clave JWT tiene {} bits. Mínimo 256 bits requeridos.", keyBytes.length * 8);
            log.error("🔑 Longitud de la clave: {} caracteres", secret.length());
            throw new IllegalStateException(
                    String.format("Clave JWT insegura: %d bits. Se requieren mínimo 256 bits.", keyBytes.length * 8)
            );
        }

        // ✅ Crear clave secreta
        this.clave = Keys.hmacShaKeyFor(keyBytes);
        log.info("✅ JWT Service inicializado correctamente");
        log.info("🔑 Clave JWT: {} bits ({} caracteres)", keyBytes.length * 8, secret.length());
    }

    public String generarToken(Long usuarioId, String email) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(email)
                .claim("usuarioId", usuarioId)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(clave)
                .compact();
    }

    public String extraerEmail(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public Long extraerUsuarioId(String token) {
        return extraerTodosLosClaims(token).get("usuarioId", Long.class);
    }

    public boolean esTokenValido(String token, String emailEsperado) {
        try {
            String email = extraerEmail(token);
            return email.equals(emailEsperado) && !estaExpirado(token);
        } catch (Exception e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    private boolean estaExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extraerTodosLosClaims(token);
        return resolver.apply(claims);
    }

    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}