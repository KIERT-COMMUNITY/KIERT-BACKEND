// src/main/java/com/kiert/backend/service/CodigoVerificacionService.java
package com.kiert.backend.features.passwordYcodigo.service;

import com.kiert.backend.entity.CodigoVerificacion;
import com.kiert.backend.entity.TipoCodigo;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.features.passwordYcodigo.repository.CodigoVerificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodigoVerificacionService {

    private final CodigoVerificacionRepository codigoRepository;
    private final RedisTemplate<String, Object> redis;

    private static final SecureRandom random = new SecureRandom();

    // ============================================================
    // CLAVES REDIS PARA RATE LIMITING
    // ============================================================
    private static final String KEY_GENERAR_POR_EMAIL = "ratelimit:codigo:gen:email:";
    private static final String KEY_GENERAR_POR_IP = "ratelimit:codigo:gen:ip:";
    private static final String KEY_INTENTOS_VALIDAR = "ratelimit:codigo:val:email:";

    // Límites
    private static final int MAX_GENERACIONES_POR_EMAIL = 3;
    private static final Duration VENTANA_EMAIL = Duration.ofHours(1);

    private static final int MAX_GENERACIONES_POR_IP = 10;
    private static final Duration VENTANA_IP = Duration.ofHours(1);

    private static final int MAX_INTENTOS_VALIDACION = 5;
    private static final Duration VENTANA_VALIDACION = Duration.ofMinutes(15);

    // ============================================================
    // GENERAR CÓDIGO
    // ============================================================
    @Transactional
    public String generarCodigo(String email, TipoCodigo tipo) {
        return generarCodigo(email, tipo, null);
    }

    @Transactional
    public String generarCodigo(String email, TipoCodigo tipo, String ipCliente) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email requerido");
        }
        if (tipo == null) {
            throw new BadRequestException("Tipo de código requerido");
        }

        // 1. Rate limiting por email
        verificarLimiteGeneracion(email);

        // 2. Rate limiting por IP
        if (ipCliente != null && !ipCliente.isBlank()) {
            verificarLimiteGeneracionPorIp(ipCliente);
        }

        // 3. Invalidar códigos anteriores
        codigoRepository.invalidarCodigosAnteriores(email, tipo, Instant.now());

        // 4. Generar código
        String codigo = String.format("%06d", random.nextInt(1_000_000));

        CodigoVerificacion entity = CodigoVerificacion.builder()
                .email(email)
                .codigo(codigo)
                .tipo(tipo)
                .build();

        codigoRepository.save(entity);

        // 5. Incrementar contadores Redis
        incrementarContador(KEY_GENERAR_POR_EMAIL + email, VENTANA_EMAIL);
        if (ipCliente != null && !ipCliente.isBlank()) {
            incrementarContador(KEY_GENERAR_POR_IP + ipCliente, VENTANA_IP);
        }

        // 6. Resetear intentos de validación (nuevo código = nueva ventana)
        redis.delete(KEY_INTENTOS_VALIDAR + email);

        // ⚠️ NUNCA loguear el código en claro. Solo el hecho de que se generó.
        log.info("Código generado para {} ({})", enmascararEmail(email), tipo);
        return codigo;
    }

    // ============================================================
    // VALIDAR Y CONSUMIR
    // ============================================================
    @Transactional
    public void validarYConsumir(String email, String codigo, TipoCodigo tipo) {
        if (email == null || email.isBlank() || codigo == null || codigo.isBlank() || tipo == null) {
            throw new BadRequestException("Código incorrecto o expirado");
        }

        // 1. Rate limiting por intentos
        verificarLimiteIntentosValidacion(email);

        // 2. Buscar código en MySQL
        CodigoVerificacion entity = codigoRepository
                .findCodigoVigente(email, codigo, tipo, Instant.now())
                .orElse(null);

        if (entity == null) {
            incrementarContador(KEY_INTENTOS_VALIDAR + email, VENTANA_VALIDACION);
            throw new BadRequestException("Código incorrecto o expirado");
        }

        entity.setUsado(true);
        entity.setFechaUso(Instant.now());
        codigoRepository.save(entity);

        // 3. Limpiar contador
        redis.delete(KEY_INTENTOS_VALIDAR + email);

        log.info("Código validado y consumido para {} ({})",
                enmascararEmail(email), tipo);
    }

    // ============================================================
    // HELPERS DE RATE LIMITING
    // ============================================================

    private void verificarLimiteGeneracion(String email) {
        String key = KEY_GENERAR_POR_EMAIL + email;
        Object valor = redis.opsForValue().get(key);

        if (valor != null) {
            long actual = parsearLong(valor);
            if (actual >= MAX_GENERACIONES_POR_EMAIL) {
                Long ttl = redis.getExpire(key);
                long minutos = ttl != null && ttl > 0 ? (ttl + 59) / 60 : 0;
                throw new BadRequestException(
                        "Has solicitado demasiados códigos. Intenta de nuevo en " + minutos + " minutos."
                );
            }
        }
    }

    private void verificarLimiteGeneracionPorIp(String ip) {
        String key = KEY_GENERAR_POR_IP + ip;
        Object valor = redis.opsForValue().get(key);

        if (valor != null) {
            long actual = parsearLong(valor);
            if (actual >= MAX_GENERACIONES_POR_IP) {
                Long ttl = redis.getExpire(key);
                long minutos = ttl != null && ttl > 0 ? (ttl + 59) / 60 : 0;
                throw new BadRequestException(
                        "Demasiadas solicitudes desde tu conexión. Intenta de nuevo en " + minutos + " minutos."
                );
            }
        }
    }

    private void verificarLimiteIntentosValidacion(String email) {
        String key = KEY_INTENTOS_VALIDAR + email;
        Object valor = redis.opsForValue().get(key);

        if (valor != null) {
            long actual = parsearLong(valor);
            if (actual >= MAX_INTENTOS_VALIDACION) {
                Long ttl = redis.getExpire(key);
                long minutos = ttl != null && ttl > 0 ? (ttl + 59) / 60 : 0;
                throw new BadRequestException(
                        "Demasiados intentos fallidos. Intenta de nuevo en " + minutos + " minutos."
                );
            }
        }
    }

    private void incrementarContador(String key, Duration ttl) {
        try {
            Long nuevoValor = redis.opsForValue().increment(key);
            if (nuevoValor != null && nuevoValor == 1L) {
                redis.expire(key, ttl);
            }
        } catch (Exception e) {
            log.error("Error incrementando contador Redis: {}", e.getMessage());
        }
    }

    private long parsearLong(Object valor) {
        if (valor instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(valor.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Enmascara el email para no exponerlo completo en logs.
     */
    private String enmascararEmail(String email) {
        if (email == null || email.isBlank()) return "***";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String dominio = email.substring(at + 1);
        String localMask = local.length() <= 1 ? "***" : local.charAt(0) + "***";
        int dot = dominio.lastIndexOf('.');
        String dominioMask = dot <= 0
                ? "***"
                : dominio.charAt(0) + "***" + dominio.substring(dot);
        return localMask + "@" + dominioMask;
    }
}