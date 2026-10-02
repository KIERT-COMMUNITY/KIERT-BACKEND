// src/main/java/com/kiert/backend/service/PresenciaService.java
package com.kiert.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

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
    // MARCAR EN LÍNEA
    // ============================================================
    public void marcarEnLinea(Long usuarioId) {
        if (usuarioId == null) return;
        try {
            String keyOnline = KEY_ONLINE + usuarioId;
            String keyUltima = KEY_ULTIMA + usuarioId;

            // Online con TTL de 5 min (si no se renueva, expira solo)
            redis.opsForValue().set(keyOnline, "1", TTL_ONLINE);

            // Última conexión con TTL de 7 días
            redis.opsForValue().set(keyUltima, Instant.now().toString(), TTL_ULTIMA);

            log.debug("Usuario {} EN LÍNEA (Redis)", usuarioId);
        } catch (Exception e) {
            log.error("Error marcando en línea a {} en Redis: {}", usuarioId, e.getMessage());
        }
    }

    // ============================================================
    // MARCAR DESCONECTADO
    // ============================================================
    public void marcarDesconectado(Long usuarioId) {
        if (usuarioId == null) return;
        try {
            // Borrar clave de online (inmediato)
            redis.delete(KEY_ONLINE + usuarioId);

            // Actualizar última conexión
            redis.opsForValue().set(
                    KEY_ULTIMA + usuarioId,
                    Instant.now().toString(),
                    TTL_ULTIMA
            );

            log.debug("Usuario {} DESCONECTADO (Redis)", usuarioId);
        } catch (Exception e) {
            log.error("Error marcando desconectado a {} en Redis: {}", usuarioId, e.getMessage());
        }
    }

    // ============================================================
    // CONSULTAR ESTADO
    // ============================================================
    public boolean estaEnLinea(Long usuarioId) {
        if (usuarioId == null) return false;
        try {
            return Boolean.TRUE.equals(redis.hasKey(KEY_ONLINE + usuarioId));
        } catch (Exception e) {
            log.error("Error consultando presencia de {}: {}", usuarioId, e.getMessage());
            return false;
        }
    }

    public Instant obtenerUltimaConexion(Long usuarioId) {
        if (usuarioId == null) return null;
        try {
            String valor = redis.opsForValue().get(KEY_ULTIMA + usuarioId);
            return valor != null ? Instant.parse(valor) : null;
        } catch (Exception e) {
            log.error("Error consultando última conexión de {}: {}", usuarioId, e.getMessage());
            return null;
        }
    }

    /**
     * Devuelve "en línea" o "últ. vez hace X" en una sola llamada.
     */
    public String getTextoEstado(Long usuarioId) {
        if (estaEnLinea(usuarioId)) return "En línea";

        Instant ultima = obtenerUltimaConexion(usuarioId);
        if (ultima == null) return "Desconectado";

        long segundos = Instant.now().getEpochSecond() - ultima.getEpochSecond();
        if (segundos < 60) return "Últ. vez hace unos segundos";
        long minutos = segundos / 60;
        if (minutos < 60) return "Últ. vez hace " + minutos + " min";
        long horas = minutos / 60;
        if (horas < 24) return "Últ. vez hace " + horas + " h";
        long dias = horas / 24;
        if (dias < 7) return "Últ. vez hace " + dias + " d";
        return "Últ. vez " + ultima.toString().substring(0, 10);
    }
}