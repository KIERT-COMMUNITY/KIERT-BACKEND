package com.kiert.backend.service;

import com.kiert.backend.entity.CodigoVerificacion;
import com.kiert.backend.entity.TipoCodigo;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.repository.CodigoVerificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;

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
    private static final int MAX_GENERACIONES_POR_EMAIL = 3;      // 3 códigos por hora
    private static final Duration VENTANA_EMAIL = Duration.ofHours(1);

    private static final int MAX_GENERACIONES_POR_IP = 10;        // 10 códigos por hora por IP
    private static final Duration VENTANA_IP = Duration.ofHours(1);

    private static final int MAX_INTENTOS_VALIDACION = 5;         // 5 intentos por código
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
        // 1️⃣ Rate limiting por email
        verificarLimiteGeneracion(email);

        // 2️⃣ Rate limiting por IP (si se proporciona)
        if (ipCliente != null && !ipCliente.isBlank()) {
            verificarLimiteGeneracionPorIp(ipCliente);
        }

        // 3️⃣ Invalidar códigos anteriores (MySQL)
        codigoRepository.invalidarCodigosAnteriores(email, tipo);

        // 4️⃣ Generar código
        String codigo = String.format("%06d", random.nextInt(1_000_000));

        CodigoVerificacion entity = CodigoVerificacion.builder()
                .email(email)
                .codigo(codigo)
                .tipo(tipo)
                .build();

        codigoRepository.save(entity);

        // 5️⃣ Incrementar contadores Redis
        incrementarContador(KEY_GENERAR_POR_EMAIL + email, VENTANA_EMAIL);
        if (ipCliente != null && !ipCliente.isBlank()) {
            incrementarContador(KEY_GENERAR_POR_IP + ipCliente, VENTANA_IP);
        }

        // 6️⃣ Resetear intentos de validación (nuevo código = nueva ventana)
        redis.delete(KEY_INTENTOS_VALIDAR + email);

        log.info("Código generado para {} ({}): {}", email, tipo, codigo);
        return codigo;
    }

    // ============================================================
    // VALIDAR Y CONSUMIR
    // ============================================================
    @Transactional
    public void validarYConsumir(String email, String codigo, TipoCodigo tipo) {
        // 1️⃣ Rate limiting por intentos de validación
        verificarLimiteIntentosValidacion(email);

        // 2️⃣ Buscar código en MySQL
        CodigoVerificacion entity = codigoRepository
                .findTopByEmailAndCodigoAndTipoAndUsadoFalseOrderByFechaCreacionDesc(email, codigo, tipo)
                .orElseThrow(() -> {
                    incrementarContador(KEY_INTENTOS_VALIDAR + email, VENTANA_VALIDACION);
                    return new BadRequestException("Codigo incorrecto o expirado");
                });

        if (!entity.estaVigente()) {
            incrementarContador(KEY_INTENTOS_VALIDAR + email, VENTANA_VALIDACION);
            throw new BadRequestException("El codigo ha expirado. Solicita uno nuevo.");
        }

        entity.setUsado(true);
        codigoRepository.save(entity);

        // 3️⃣ Limpiar contador de intentos al validar con éxito
        redis.delete(KEY_INTENTOS_VALIDAR + email);

        log.info("Código validado y consumido para {} ({})", email, tipo);
    }

    // ============================================================
    // HELPERS DE RATE LIMITING
    // ============================================================

    private void verificarLimiteGeneracion(String email) {
        String key = KEY_GENERAR_POR_EMAIL + email;
        Object valor = redis.opsForValue().get(key);

        if (valor != null) {
            long actual = ((Number) valor).longValue();
            if (actual >= MAX_GENERACIONES_POR_EMAIL) {
                Long ttl = redis.getExpire(key);
                long minutos = ttl != null && ttl > 0 ? ttl / 60 : 0;
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
            long actual = ((Number) valor).longValue();
            if (actual >= MAX_GENERACIONES_POR_IP) {
                Long ttl = redis.getExpire(key);
                long minutos = ttl != null && ttl > 0 ? ttl / 60 : 0;
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
            long actual = ((Number) valor).longValue();
            if (actual >= MAX_INTENTOS_VALIDACION) {
                Long ttl = redis.getExpire(key);
                long minutos = ttl != null && ttl > 0 ? ttl / 60 : 0;
                throw new BadRequestException(
                        "Demasiados intentos fallidos. Intenta de nuevo en " + minutos + " minutos."
                );
            }
        }
    }

    /**
     * Incrementa el contador en Redis. Si es la primera vez, establece TTL.
     */
    private void incrementarContador(String key, Duration ttl) {
        Long nuevoValor = redis.opsForValue().increment(key);
        // Si es la primera vez (valor = 1), establecer TTL
        if (nuevoValor != null && nuevoValor == 1L) {
            redis.expire(key, ttl);
        }
    }
}