// src/main/java/com/kiert/backend/features/auth/service/RateLimitService.java
package com.kiert.backend.features.auth.service;

import com.kiert.backend.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redis;

    public void verificar(String clave, int maxIntentos, Duration ventana) {
        String key = "ratelimit:" + clave;
        try {
            String valor = redis.opsForValue().get(key);
            long actual = valor != null ? Long.parseLong(valor) : 0;

            if (actual >= maxIntentos) {
                Long ttlSegundos = redis.getExpire(key);
                long minutos = ttlSegundos != null && ttlSegundos > 0
                        ? (ttlSegundos + 59) / 60
                        : 0;
                throw new TooManyRequestsException(
                        "Demasiados intentos. Intenta de nuevo en " + minutos + " minutos."
                );
            }

            Long nuevo = redis.opsForValue().increment(key);
            if (nuevo != null && nuevo == 1L) {
                redis.expire(key, ventana);
            }
        } catch (TooManyRequestsException e) {
            throw e;
        } catch (Exception e) {
            // Fail-open: si Redis falla, permitimos la acción
            log.error("[RATE LIMIT] Error en {}: {}", clave, e.getMessage());
        }
    }

    public void resetear(String clave) {
        try {
            redis.delete("ratelimit:" + clave);
        } catch (Exception e) {
            log.error("[RATE LIMIT] Error reseteando {}: {}", clave, e.getMessage());
        }
    }

    public boolean estaBloqueado(String clave, int maxIntentos) {
        try {
            String valor = redis.opsForValue().get("ratelimit:" + clave);
            return valor != null && Long.parseLong(valor) >= maxIntentos;
        } catch (Exception e) {
            return false;
        }
    }
}