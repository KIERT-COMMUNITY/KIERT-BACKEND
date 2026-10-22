// src/main/java/com/kiert/backend/service/auth/AuthService.java
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
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

    private static final List<String> ROLES_ADMIN = List.of("CEO", "ADMIN", "MODERADOR");

    // ============================================================
    // REGISTRO
    // ============================================================
    @Transactional
    public void registrar(RegisterRequestDTO datos) {
        log.info("Registrando usuario: {}", datos.email());

        rateLimitService.verificar(
                "registro:email:" + datos.email(),
                3,
                Duration.ofHours(1)
        );

        // Rate limit por IP también
        rateLimitService.verificar(
                "registro:ip:" + obtenerIpCliente(),
                10,
                Duration.ofHours(1)
        );

        if (usuarioRepository.existsByEmail(datos.email())) {
            auditoriaService.registrar(
                    null,
                    datos.email(),
                    EventoAuditoria.REGISTRO,
                    "Intento de registro con email ya existente",
                    false
            );
            throw new BadRequestException("El email ya está registrado");
        }

        if (usuarioRepository.existsByNombreUsuario(datos.nombreUsuario())) {
            auditoriaService.registrar(
                    null,
                    datos.email(),
                    EventoAuditoria.REGISTRO,
                    "Intento de registro con nombre de usuario ya existente",
                    false
            );
            throw new BadRequestException("El nombre de usuario ya está en uso");
        }

        Usuario usuario = Usuario.builder()
                .nombreUsuario(datos.nombreUsuario())
                .email(datos.email())
                .passwordHash(passwordEncoder.encode(datos.password()))
                .fechaCreacion(Instant.now())
                .enLinea(false)
                .activo(false)
                .emailVerificado(false)
                .rol("USER") // ✅ Explícito para evitar nulls
                .build();

        usuario = usuarioRepository.save(usuario);

        String codigo = codigoVerificacionService.generarCodigo(
                datos.email(), TipoCodigo.VERIFICACION_CUENTA);

        emailService.enviarCodigoVerificacion(
                datos.email(), datos.nombreUsuario(), codigo);

        auditoriaService.registrar(
                usuario.getId(),
                datos.email(),
                EventoAuditoria.REGISTRO,
                "Usuario registrado. Código de verificación enviado al email.",
                true
        );

        log.info("Usuario registrado (pendiente verificación): {}", datos.email());
    }

    // ============================================================
    // VERIFICAR CUENTA
    // ============================================================
    @Transactional
    public void verificarCuenta(String email, String codigo) {
        log.info("Verificando cuenta: {}", email);

        rateLimitService.verificar(
                "verificar:email:" + email,
                5,
                Duration.ofMinutes(15)
        );

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
            auditoriaService.registrar(
                    usuario.getId(),
                    email,
                    EventoAuditoria.VERIFICACION_EMAIL,
                    "Intento fallido: código incorrecto o expirado",
                    false
            );
            throw e;
        }

        rateLimitService.resetear("verificar:email:" + email);

        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setFechaVerificacionEmail(Instant.now());
        usuarioRepository.save(usuario);

        emailService.enviarCorreoBienvenida(email, usuario.getNombreUsuario());

        auditoriaService.registrar(
                usuario.getId(),
                email,
                EventoAuditoria.VERIFICACION_EMAIL,
                "Cuenta verificada correctamente. Email de bienvenida enviado.",
                true
        );

        log.info("Cuenta verificada y bienvenida enviada: {}", email);
    }

    // ============================================================
    // REENVIAR CÓDIGO DE VERIFICACIÓN
    // ============================================================
    @Transactional
    public void reenviarCodigoVerificacion(String email) {
        log.info("Reenviando código de verificación: {}", email);

        rateLimitService.verificar(
                "reenvio:email:" + email,
                3,
                Duration.ofHours(1)
        );

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadRequestException("Esta cuenta ya está activa");
        }

        String codigo = codigoVerificacionService.generarCodigo(
                email, TipoCodigo.VERIFICACION_CUENTA);

        emailService.enviarCodigoVerificacion(email, usuario.getNombreUsuario(), codigo);

        auditoriaService.registrar(
                usuario.getId(),
                email,
                EventoAuditoria.REENVIO_CODIGO,
                "Código de verificación reenviado al email",
                true
        );
    }

    // ============================================================
    // LOGIN — PROTEGIDO CONTRA TIMING ATTACKS
    // ============================================================
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO datos) {
        log.info("Login para usuario: {}", datos.email());

        // Rate limiting por email y por IP
        rateLimitService.verificar(
                "login:email:" + datos.email(),
                5,
                Duration.ofMinutes(15)
        );

        rateLimitService.verificar(
                "login:ip:" + obtenerIpCliente(),
                20,
                Duration.ofMinutes(15)
        );

        // ============================================================
        // TIMING ATTACK: Buscamos el usuario pero SIEMPRE ejecutamos
        // el mismo flujo, incluso si el usuario no existe.
        // ============================================================
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(datos.email());

        // Si no existe, ejecutamos un BCrypt ficticio para simular el tiempo
        // de comparación real y evitar enumeración de emails.
        if (usuarioOpt.isEmpty()) {
            passwordEncoder.matches(datos.password(), "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
            auditoriaService.registrar(
                    null,
                    datos.email(),
                    EventoAuditoria.LOGIN_FALLIDO,
                    "Intento de login con email no registrado",
                    false
            );
            throw new BadRequestException("Credenciales inválidas");
        }

        Usuario usuario = usuarioOpt.get();

        // ============================================================
        // MENSAJE GENÉRICO: no revelar si la cuenta está inactiva o es admin
        // ============================================================
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            auditoriaService.registrar(
                    usuario.getId(),
                    datos.email(),
                    EventoAuditoria.CUENTA_NO_VERIFICADA,
                    "Intento de login con cuenta sin verificar",
                    false
            );
            throw new BadRequestException("Credenciales inválidas");
        }

        // ============================================================
        // RECHAZAR ADMINS EN LOGIN NORMAL
        // ============================================================
        String rol = usuario.getRol() != null ? usuario.getRol() : "USER";
        if (ROLES_ADMIN.contains(rol)) {
            log.warn("Intento de login normal con cuenta admin: {}", datos.email());
            auditoriaService.registrar(
                    usuario.getId(),
                    datos.email(),
                    EventoAuditoria.LOGIN_FALLIDO,
                    "Intento de login normal con cuenta de administrador",
                    false
            );
            throw new BadRequestException("Credenciales inválidas");
        }

        // Autenticación real (valida contraseña)
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(datos.email(), datos.password())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            auditoriaService.registrar(
                    usuario.getId(),
                    datos.email(),
                    EventoAuditoria.LOGIN_FALLIDO,
                    "Contraseña incorrecta",
                    false
            );
            throw new BadRequestException("Credenciales inválidas");
        }

        rateLimitService.resetear("login:email:" + datos.email());
        rateLimitService.resetear("login:ip:" + obtenerIpCliente());

        usuarioService.marcarEnLinea(usuario.getId());
        log.info("Usuario {} marcado como EN LÍNEA tras login", usuario.getId());

        auditoriaService.registrar(
                usuario.getId(),
                datos.email(),
                EventoAuditoria.LOGIN,
                "Inicio de sesión correcto",
                true
        );

        // ============================================================
        // TOKEN CON ROL: siempre incluir el rol actual
        // ============================================================
        String token = jwtService.generarTokenConRol(
                usuario.getId(),
                usuario.getEmail(),
                rol
        );

        return new AuthResponseDTO(token, aDTO(usuario));
    }

    // ============================================================
    // LOGOUT (con blacklist de JWT)
    // ============================================================
    @Transactional
    public void logout(Long usuarioId, String token) {
        log.info("Cerrando sesión para usuario: {}", usuarioId);

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

            auditoriaService.registrar(
                    usuarioId,
                    null,
                    EventoAuditoria.LOGOUT,
                    "Cierre de sesión",
                    true
            );
        }
    }

    // ============================================================
    // SOLICITAR RECUPERACIÓN
    // ============================================================
    @Transactional
    public void solicitarRecuperacion(String email) {
        log.info("Solicitando recuperación para: {}", email);

        rateLimitService.verificar(
                "recuperacion:email:" + email,
                3,
                Duration.ofHours(1)
        );

        rateLimitService.verificar(
                "recuperacion:ip:" + obtenerIpCliente(),
                10,
                Duration.ofHours(1)
        );

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    auditoriaService.registrar(
                            null,
                            email,
                            EventoAuditoria.SOLICITUD_RECUPERACION,
                            "Solicitud de recuperación para email no registrado",
                            false
                    );
                    // MENSAJE GENÉRICO: no revelar si el email existe
                    return new BadRequestException(
                            "Si el correo está registrado, recibirás un código."
                    );
                });

        String codigo = codigoVerificacionService.generarCodigo(
                email, TipoCodigo.RECUPERACION_PASSWORD);

        emailService.enviarCodigoRecuperacion(email, usuario.getNombreUsuario(), codigo);

        auditoriaService.registrar(
                usuario.getId(),
                email,
                EventoAuditoria.SOLICITUD_RECUPERACION,
                "Código de recuperación enviado al email",
                true
        );

        log.info("Código de recuperación enviado a: {}", email);
    }

    // ============================================================
    // RESET PASSWORD CON CÓDIGO
    // ============================================================
    @Transactional
    public void restablecerPasswordConCodigo(String email, String codigo, String nuevaPassword) {
        log.info("Restableciendo contraseña para: {}", email);

        rateLimitService.verificar(
                "reset:email:" + email,
                5,
                Duration.ofMinutes(15)
        );

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        try {
            codigoVerificacionService.validarYConsumir(
                    email, codigo, TipoCodigo.RECUPERACION_PASSWORD);
        } catch (Exception e) {
            auditoriaService.registrar(
                    usuario.getId(),
                    email,
                    EventoAuditoria.CAMBIO_PASSWORD,
                    "Intento fallido: código incorrecto o expirado",
                    false
            );
            throw e;
        }

        rateLimitService.resetear("reset:email:" + email);

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setFechaUltimoCambioPassword(Instant.now());
        usuarioRepository.save(usuario);

        // Invalidar TODOS los tokens del usuario (forzar re-login)
        // Nota: necesitarías un tokenVersion o usar Redis con patrón de usuario
        // para invalidar todos los tokens. Por ahora no se implementa.

        auditoriaService.registrar(
                usuario.getId(),
                email,
                EventoAuditoria.CAMBIO_PASSWORD,
                "Contraseña restablecida correctamente con código",
                true
        );

        log.info("Contraseña restablecida para {}", email);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /**
     * Obtiene la IP del cliente desde la request actual.
     * Si no hay request (ej. tests), devuelve "unknown".
     */
    private String obtenerIpCliente() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";

            HttpServletRequest request = attrs.getRequest();

            // Considerar proxy (X-Forwarded-For)
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
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