package com.kiert.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

// Genera y valida los JWT que consume auth.interceptor.ts en el frontend
// (header "Authorization: Bearer <token>").
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(JwtProperties jwtProperties) {
        this.clave = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = jwtProperties.expirationMs();
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
        String email = extraerEmail(token);
        return email.equals(emailEsperado) && !estaExpirado(token);
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
