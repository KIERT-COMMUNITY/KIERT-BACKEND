// src/main/java/com/kiert/backend/features/presencia/service/PresenciaService.java
package com.kiert.backend.features.presencia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenciaService {

    private final StringRedisTemplate redis;

    private static final String KEY_ONLINE = "presencia:online:";
    private static final String KEY_ULTIMA = "presencia:ultima:";
    private static final Duration TTL_ONLINE = Duration.ofMinutes(5);
    private static final Duration TTL_ULTIMA = Duration.ofDays(7);

    // ============================================================
    // MARCAR EN LINEA
    // ============================================================
    public void marcarEnLinea(Long usuarioId) {
        if (usuarioId == null) return;
        try {
            redis.opsForValue().set(KEY_ONLINE + usuarioId, "1", TTL_ONLINE);
            redis.opsForValue().set(KEY_ULTIMA + usuarioId, Instant.now().toString(), TTL_ULTIMA);
        } catch (Exception e) {
            log.warn("Error marcando en linea a {}: {}", usuarioId, e.getMessage());
        }
    }

    // ============================================================
    // MARCAR DESCONECTADO
    // ============================================================
    public void marcarDesconectado(Long usuarioId) {
        if (usuarioId == null) return;
        try {
            redis.delete(KEY_ONLINE + usuarioId);
            redis.opsForValue().set(KEY_ULTIMA + usuarioId, Instant.now().toString(), TTL_ULTIMA);
        } catch (Exception e) {
            log.warn("Error marcando desconectado a {}: {}", usuarioId, e.getMessage());
        }
    }

    // ============================================================
    // CONSULTAR
    // ============================================================
    public boolean estaEnLinea(Long usuarioId) {
        if (usuarioId == null) return false;
        try {
            return Boolean.TRUE.equals(redis.hasKey(KEY_ONLINE + usuarioId));
        } catch (Exception e) {
            log.warn("Error consultando presencia de {}: {}", usuarioId, e.getMessage());
            return false;
        }
    }

    public Instant obtenerUltimaConexion(Long usuarioId) {
        if (usuarioId == null) return null;
        try {
            String valor = redis.opsForValue().get(KEY_ULTIMA + usuarioId);
            return valor != null ? Instant.parse(valor) : null;
        } catch (Exception e) {
            log.warn("Error obteniendo ultima conexion de {}: {}", usuarioId, e.getMessage());
            return null;
        }
    }

    public Map<Long, EstadoPresencia> obtenerEstados(List<Long> usuarioIds) {
        Map<Long, EstadoPresencia> resultado = new HashMap<>();
        if (usuarioIds == null || usuarioIds.isEmpty()) return resultado;

        for (Long id : usuarioIds) {
            if (id == null) continue;
            try {
                resultado.put(id, new EstadoPresencia(
                        estaEnLinea(id),
                        obtenerUltimaConexion(id)
                ));
            } catch (Exception e) {
                log.warn("Error obteniendo estado de {}: {}", id, e.getMessage());
                resultado.put(id, new EstadoPresencia(false, null));
            }
        }
        return resultado;
    }

    public record EstadoPresencia(boolean enLinea, Instant ultimaConexion) {}
}