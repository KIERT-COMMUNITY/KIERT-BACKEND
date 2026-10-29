// src/main/java/com/kiert/backend/features/passwordYcodigo/service/CodigoVerificacionService.java
package com.kiert.backend.features.passwordYcodigo.service;

import com.kiert.backend.entity.CodigoVerificacion;
import com.kiert.backend.entity.TipoCodigo;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.features.passwordYcodigo.repository.CodigoVerificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodigoVerificacionService {

    private final CodigoVerificacionRepository codigoRepository;
    private final RedisTemplate<String, Object> redis;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String KEY_GENERAR_POR_EMAIL = "ratelimit:codigo:gen:email:";
    private static final String KEY_GENERAR_POR_IP = "ratelimit:codigo:gen:ip:";
    private static final String KEY_INTENTOS_VALIDAR = "ratelimit:codigo:val:email:";

    private static final int MAX_GENERACIONES_POR_EMAIL = 3;
    private static final Duration VENTANA_EMAIL = Duration.ofHours(1);

    private static final int MAX_GENERACIONES_POR_IP = 10;
    private static final Duration VENTANA_IP = Duration.ofHours(1);

    private static final int MAX_INTENTOS_VALIDACION = 5;
    private static final Duration VENTANA_VALIDACION = Duration.ofMinutes(15);

    // ============================================================
    // GENERAR CODIGO
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
            throw new BadRequestException("Tipo de codigo requerido");
        }

        verificarLimiteGeneracion(email);

        if (ipCliente != null && !ipCliente.isBlank()) {
            verificarLimiteGeneracionPorIp(ipCliente);
        }

        codigoRepository.invalidarCodigosAnteriores(email, tipo, Instant.now());

        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));

        // SEGURIDAD: hashear el codigo antes de guardarlo.
        // El codigo en claro solo viaja al email del usuario.
        String codigoHash = passwordEncoder.encode(codigo);

        CodigoVerificacion entity = CodigoVerificacion.builder()
                .email(email)
                .codigo(codigoHash)
                .tipo(tipo)
                .build();

        codigoRepository.save(entity);

        incrementarContador(KEY_GENERAR_POR_EMAIL + email, VENTANA_EMAIL);
        if (ipCliente != null && !ipCliente.isBlank()) {
            incrementarContador(KEY_GENERAR_POR_IP + ipCliente, VENTANA_IP);
        }

        redis.delete(KEY_INTENTOS_VALIDAR + email);

        log.info("Codigo generado para {} ({})", enmascararEmail(email), tipo);
        return codigo;
    }

    // ============================================================
    // VALIDAR Y CONSUMIR
    // ============================================================
    @Transactional
    public void validarYConsumir(String email, String codigo, TipoCodigo tipo) {
        if (email == null || email.isBlank() || codigo == null || codigo.isBlank() || tipo == null) {
            throw new BadRequestException("Codigo incorrecto o expirado");
        }

        verificarLimiteIntentosValidacion(email);

        // SEGURIDAD: no se puede buscar por codigo en claro porque esta hasheado.
        // Se traen los codigos vigentes del email+tipo y se compara con BCrypt.
        var vigentes = codigoRepository.findCodigosVigentes(email, tipo, Instant.now());

        CodigoVerificacion entity = null;
        for (CodigoVerificacion c : vigentes) {
            if (passwordEncoder.matches(codigo, c.getCodigo())) {
                entity = c;
                break;
            }
        }

        if (entity == null) {
            incrementarContador(KEY_INTENTOS_VALIDAR + email, VENTANA_VALIDACION);
            throw new BadRequestException("Codigo incorrecto o expirado");
        }

        entity.setUsado(true);
        entity.setFechaUso(Instant.now());
        codigoRepository.save(entity);

        redis.delete(KEY_INTENTOS_VALIDAR + email);

        log.info("Codigo validado y consumido para {} ({})",
                enmascararEmail(email), tipo);
    }

    // ============================================================
    // RATE LIMITING
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
                        "Has solicitado demasiados codigos. Intenta de nuevo en " + minutos + " minutos."
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
                        "Demasiadas solicitudes desde tu conexion. Intenta de nuevo en " + minutos + " minutos."
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
            log.warn("Error incrementando contador Redis: {}", e.getMessage());
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