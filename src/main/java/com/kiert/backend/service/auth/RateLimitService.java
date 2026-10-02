package com.kiert.backend.service.auth;

import com.kiert.backend.exception.BadRequestException;
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

    /**
     * Verifica si una acción está permitida. Si no, lanza BadRequestException.
     * Si está permitida, incrementa el contador.
     *
     * @param clave     Identificador único (ej. "login:email:juan@x.com")
     * @param maxIntentos Máximo de intentos permitidos en la ventana
     * @param ventana   Duración de la ventana
     */
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
                throw new BadRequestException(
                        "Demasiados intentos. Intenta de nuevo en " + minutos + " minutos."
                );
            }

            Long nuevo = redis.opsForValue().increment(key);
            if (nuevo != null && nuevo == 1L) {
                redis.expire(key, ventana);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            // Fail-open: si Redis falla, permitimos la acción
            log.error("⚠️ Error en rate limit ({}): {}", clave, e.getMessage());
        }
    }

    /**
     * Resetea el contador (ej. tras login exitoso).
     */
    public void resetear(String clave) {
        try {
            redis.delete("ratelimit:" + clave);
        } catch (Exception e) {
            log.error("⚠️ Error reseteando rate limit ({}): {}", clave, e.getMessage());
        }
    }

    /**
     * Verifica sin incrementar (útil para consultar sin modificar).
     */
    public boolean estaBloqueado(String clave, int maxIntentos) {
        try {
            String valor = redis.opsForValue().get("ratelimit:" + clave);
            return valor != null && Long.parseLong(valor) >= maxIntentos;
        } catch (Exception e) {
            return false;
        }
    }
}