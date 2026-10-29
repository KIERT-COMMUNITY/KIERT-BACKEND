// src/main/java/com/kiert/backend/features/auth/service/AuthService.java
package com.kiert.backend.features.auth.service;

import com.kiert.backend.features.usuario.dto.UsuarioDTO;
import com.kiert.backend.features.login.request.LoginRequestDTO;
import com.kiert.backend.features.login.request.RegisterRequestDTO;
import com.kiert.backend.features.auth.dto.response.AuthResponseDTO;
import com.kiert.backend.entity.EventoAuditoria;
import com.kiert.backend.entity.TipoCodigo;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import com.kiert.backend.security.JwtService;
import com.kiert.backend.features.auditoria.service.AuditoriaService;
import com.kiert.backend.features.passwordYcodigo.service.CodigoVerificacionService;
import com.kiert.backend.features.email.service.EmailService;
import com.kiert.backend.features.usuario.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UsuarioService usuarioService;
    private final CodigoVerificacionService codigoVerificacionService;
    private final AuditoriaService auditoriaService;
    private final RateLimitService rateLimitService;
    private final TokenBlacklistService tokenBlacklistService;
    private final StringRedisTemplate redis;

    private static final List<String> ROLES_ADMIN = List.of("CEO", "ADMIN", "MODERADOR");
    private static final String KEY_RESET_TOKEN = "password:reset:token:";
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ============================================================
    // REGISTRO
    // ============================================================
    @Transactional
    public void registrar(RegisterRequestDTO datos) {
        log.info("Registrando usuario: {}", datos.email());

        rateLimitService.verificar("registro:email:" + datos.email(), 3, Duration.ofHours(1));
        rateLimitService.verificar("registro:ip:" + obtenerIpCliente(), 10, Duration.ofHours(1));

        if (usuarioRepository.existsByEmail(datos.email())) {
            auditoriaService.registrar(null, datos.email(),
                    EventoAuditoria.REGISTRO,
                    "Intento de registro con email ya existente", false);
            throw new BadRequestException("El email ya esta registrado");
        }

        if (usuarioRepository.existsByNombreUsuario(datos.nombreUsuario())) {
            auditoriaService.registrar(null, datos.email(),
                    EventoAuditoria.REGISTRO,
                    "Intento de registro con nombre de usuario ya existente", false);
            throw new BadRequestException("El nombre de usuario ya esta en uso");
        }

        Usuario usuario = Usuario.builder()
                .nombreUsuario(datos.nombreUsuario())
                .email(datos.email())
                .passwordHash(passwordEncoder.encode(datos.password()))
                .fechaCreacion(Instant.now())
                .enLinea(false)
                .activo(false)
                .emailVerificado(false)
                .rol("USER")
                .tokenVersion(0)
                .build();

        usuario = usuarioRepository.save(usuario);

        String codigo = codigoVerificacionService.generarCodigo(
                datos.email(), TipoCodigo.VERIFICACION_CUENTA);

        emailService.enviarCodigoVerificacion(datos.email(), datos.nombreUsuario(), codigo);

        auditoriaService.registrar(usuario.getId(), datos.email(),
                EventoAuditoria.REGISTRO,
                "Usuario registrado. Codigo de verificacion enviado al email.", true);

        log.info("Usuario registrado (pendiente verificacion): {}", datos.email());
    }

    // ============================================================
    // VERIFICAR CUENTA
    // ============================================================
    @Transactional
    public void verificarCuenta(String email, String codigo) {
        log.info("Verificando cuenta: {}", email);

        rateLimitService.verificar("verificar:email:" + email, 5, Duration.ofMinutes(15));

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(usuario.getActivo())) {
            log.info("La cuenta ya estaba activa: {}", email);
            return;
        }

        try {
            codigoVerificacionService.validarYConsumir(
                    email, codigo, TipoCodigo.VERIFICACION_CUENTA);
        } catch (Exception e) {
            auditoriaService.registrar(usuario.getId(), email,
                    EventoAuditoria.VERIFICACION_EMAIL,
                    "Intento fallido: codigo incorrecto o expirado", false);
            throw e;
        }

        rateLimitService.resetear("verificar:email:" + email);

        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setFechaVerificacionEmail(Instant.now());
        usuarioRepository.save(usuario);

        emailService.enviarCorreoBienvenida(email, usuario.getNombreUsuario());

        auditoriaService.registrar(usuario.getId(), email,
                EventoAuditoria.VERIFICACION_EMAIL,
                "Cuenta verificada correctamente. Email de bienvenida enviado.", true);

        log.info("Cuenta verificada y bienvenida enviada: {}", email);
    }

    // ============================================================
    // REENVIAR CODIGO
    // ============================================================
    @Transactional
    public void reenviarCodigoVerificacion(String email) {
        log.info("Reenviando codigo de verificacion: {}", email);

        rateLimitService.verificar("reenvio:email:" + email, 3, Duration.ofHours(1));

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadRequestException("Esta cuenta ya esta activa");
        }

        String codigo = codigoVerificacionService.generarCodigo(
                email, TipoCodigo.VERIFICACION_CUENTA);

        emailService.enviarCodigoVerificacion(email, usuario.getNombreUsuario(), codigo);

        auditoriaService.registrar(usuario.getId(), email,
                EventoAuditoria.REENVIO_CODIGO,
                "Codigo de verificacion reenviado al email", true);
    }

    // ============================================================
    // LOGIN
    // ============================================================
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO datos) {
        log.info("Login para usuario: {}", datos.email());

        rateLimitService.verificar("login:email:" + datos.email(), 5, Duration.ofMinutes(15));
        rateLimitService.verificar("login:ip:" + obtenerIpCliente(), 20, Duration.ofMinutes(15));

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(datos.email());

        if (usuarioOpt.isEmpty()) {
            passwordEncoder.matches(datos.password(),
                    "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
            auditoriaService.registrar(null, datos.email(),
                    EventoAuditoria.LOGIN_FALLIDO,
                    "Intento de login con email no registrado", false);
            throw new BadRequestException("Credenciales invalidas");
        }

        Usuario usuario = usuarioOpt.get();

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            auditoriaService.registrar(usuario.getId(), datos.email(),
                    EventoAuditoria.CUENTA_NO_VERIFICADA,
                    "Intento de login con cuenta sin verificar", false);
            throw new BadRequestException("Credenciales invalidas");
        }

        String rol = usuario.getRol() != null ? usuario.getRol() : "USER";
        if (ROLES_ADMIN.contains(rol)) {
            log.warn("Intento de login normal con cuenta admin: {}", datos.email());
            auditoriaService.registrar(usuario.getId(), datos.email(),
                    EventoAuditoria.LOGIN_FALLIDO,
                    "Intento de login normal con cuenta de administrador", false);
            throw new BadRequestException("Credenciales invalidas");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(datos.email(), datos.password())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            auditoriaService.registrar(usuario.getId(), datos.email(),
                    EventoAuditoria.LOGIN_FALLIDO,
                    "Contrasena incorrecta", false);
            throw new BadRequestException("Credenciales invalidas");
        }

        rateLimitService.resetear("login:email:" + datos.email());
        rateLimitService.resetear("login:ip:" + obtenerIpCliente());

        usuarioService.marcarEnLinea(usuario.getId());

        auditoriaService.registrar(usuario.getId(), datos.email(),
                EventoAuditoria.LOGIN,
                "Inicio de sesion correcto", true);

        int tokenVersion = usuario.getTokenVersion() != null ? usuario.getTokenVersion() : 0;
        String token = jwtService.generarTokenConRol(
                usuario.getId(), usuario.getEmail(), rol, tokenVersion);

        return new AuthResponseDTO(token, aDTO(usuario));
    }

    // ============================================================
    // LOGOUT
    // ============================================================
    @Transactional
    public void logout(Long usuarioId, String token) {
        log.info("Cerrando sesion para usuario: {}", usuarioId);

        if (usuarioId != null) {
            usuarioService.marcarDesconectado(usuarioId);

            if (token != null && !token.isBlank()) {
                try {
                    Instant expiracion = jwtService.obtenerExpiracion(token);
                    tokenBlacklistService.invalidar(token, expiracion);
                } catch (Exception e) {
                    log.error("Error invalidando token en logout: {}", e.getMessage());
                }
            }

            auditoriaService.registrar(usuarioId, null,
                    EventoAuditoria.LOGOUT,
                    "Cierre de sesion", true);
        }
    }

    // ============================================================
    // SOLICITAR RECUPERACION
    // ============================================================
    @Transactional
    public void solicitarRecuperacion(String email) {
        log.info("Solicitando recuperacion para: {}", email);

        rateLimitService.verificar("recuperacion:email:" + email, 3, Duration.ofHours(1));
        rateLimitService.verificar("recuperacion:ip:" + obtenerIpCliente(), 10, Duration.ofHours(1));

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isEmpty()) {
            auditoriaService.registrar(null, email,
                    EventoAuditoria.SOLICITUD_RECUPERACION,
                    "Solicitud de recuperacion para email no registrado", false);
            return;
        }

        Usuario usuario = usuarioOpt.get();
        String codigo = codigoVerificacionService.generarCodigo(
                email, TipoCodigo.RECUPERACION_PASSWORD);

        emailService.enviarCodigoRecuperacion(email, usuario.getNombreUsuario(), codigo);

        auditoriaService.registrar(usuario.getId(), email,
                EventoAuditoria.SOLICITUD_RECUPERACION,
                "Codigo de recuperacion enviado al email", true);

        log.info("Codigo de recuperacion enviado a: {}", email);
    }

    // ============================================================
    // VERIFICAR CODIGO DE RECUPERACION Y EMITIR TOKEN TEMPORAL
    // ============================================================
    @Transactional
    public String verificarCodigoRecuperacion(String email, String codigo) {
        log.info("Verificando codigo de recuperacion para: {}", email);

        rateLimitService.verificar("verificar-reset:email:" + email, 5, Duration.ofMinutes(15));

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Codigo incorrecto o expirado"));

        try {
            codigoVerificacionService.validarYConsumir(
                    email, codigo, TipoCodigo.RECUPERACION_PASSWORD);
        } catch (Exception e) {
            auditoriaService.registrar(usuario.getId(), email,
                    EventoAuditoria.CAMBIO_PASSWORD,
                    "Intento fallido de verificacion de codigo de recuperacion", false);
            throw new BadRequestException("Codigo incorrecto o expirado");
        }

        String resetToken = generarResetToken();
        redis.opsForValue().set(
                KEY_RESET_TOKEN + resetToken,
                email,
                RESET_TOKEN_TTL
        );

        auditoriaService.registrar(usuario.getId(), email,
                EventoAuditoria.CAMBIO_PASSWORD,
                "Codigo de recuperacion verificado. Token temporal emitido.", true);

        return resetToken;
    }

    // ============================================================
    // RESET PASSWORD CON TOKEN TEMPORAL
    // ============================================================
    @Transactional
    public void restablecerPasswordConToken(String resetToken, String nuevaPassword) {
        log.info("Restableciendo password con token temporal");

        String email = redis.opsForValue().get(KEY_RESET_TOKEN + resetToken);
        if (email == null) {
            throw new BadRequestException("El enlace expiro o ya fue usado. Solicita uno nuevo.");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        redis.delete(KEY_RESET_TOKEN + resetToken);

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setFechaUltimoCambioPassword(Instant.now());
        int tokenVersionActual = usuario.getTokenVersion() != null ? usuario.getTokenVersion() : 0;
        usuario.setTokenVersion(tokenVersionActual + 1);
        usuarioRepository.save(usuario);

        auditoriaService.registrar(usuario.getId(), email,
                EventoAuditoria.CAMBIO_PASSWORD,
                "Contrasena restablecida correctamente con token temporal", true);

        log.info("Contrasena restablecida para {}", email);
    }

    // ============================================================
    // RESET PASSWORD CON CODIGO (compatibilidad flujo antiguo)
    // ============================================================
    @Transactional
    public void restablecerPasswordConCodigo(String email, String codigo, String nuevaPassword) {
        log.info("Restableciendo password para: {}", email);

        rateLimitService.verificar("reset:email:" + email, 5, Duration.ofMinutes(15));

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        try {
            codigoVerificacionService.validarYConsumir(
                    email, codigo, TipoCodigo.RECUPERACION_PASSWORD);
        } catch (Exception e) {
            auditoriaService.registrar(usuario.getId(), email,
                    EventoAuditoria.CAMBIO_PASSWORD,
                    "Intento fallido: codigo incorrecto o expirado", false);
            throw e;
        }

        rateLimitService.resetear("reset:email:" + email);

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setFechaUltimoCambioPassword(Instant.now());
        int tokenVersionActual = usuario.getTokenVersion() != null ? usuario.getTokenVersion() : 0;
        usuario.setTokenVersion(tokenVersionActual + 1);
        usuarioRepository.save(usuario);

        auditoriaService.registrar(usuario.getId(), email,
                EventoAuditoria.CAMBIO_PASSWORD,
                "Contrasena restablecida correctamente con codigo", true);

        log.info("Contrasena restablecida para {}", email);
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private String generarResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String obtenerIpCliente() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";

            HttpServletRequest request = attrs.getRequest();
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private UsuarioDTO aDTO(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl(),
                usuario.getBio()
        );
    }
}