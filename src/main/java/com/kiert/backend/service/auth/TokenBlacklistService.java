package com.kiert.backend.service.auth;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redis;

    private static final String KEY_BLACKLIST = "jwt:blacklist:";

    /**
     * Añade un JWT a la blacklist hasta su expiración natural.
     */
    public void invalidar(String token, Instant expiracion) {
        if (token == null || token.isBlank()) return;
        try {
            long segundosRestantes = expiracion.getEpochSecond() - Instant.now().getEpochSecond();
            if (segundosRestantes <= 0) {
                log.debug("Token ya expirado, no se añade a blacklist");
                return;
            }
            redis.opsForValue().set(
                    KEY_BLACKLIST + token,
                    "1",
                    Duration.ofSeconds(segundosRestantes)
            );
            log.info(" Token invalidado en blacklist (TTL {}s)", segundosRestantes);
        } catch (Exception e) {
            log.error("Error añadiendo token a blacklist: {}", e.getMessage());
        }
    }

    /**
     * Verifica si un token está en la blacklist.
     */
    public boolean estaInvalidado(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            return Boolean.TRUE.equals(redis.hasKey(KEY_BLACKLIST + token));
        } catch (Exception e) {
            log.error(" Error consultando blacklist: {}", e.getMessage());
            return false;  // fail-open
        }
    }
}