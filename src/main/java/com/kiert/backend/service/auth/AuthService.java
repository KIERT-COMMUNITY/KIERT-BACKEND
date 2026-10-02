package com.kiert.backend.service.auth;

import com.kiert.backend.dto.UsuarioDTO;
import com.kiert.backend.dto.request.LoginRequestDTO;
import com.kiert.backend.dto.request.RegisterRequestDTO;
import com.kiert.backend.dto.response.AuthResponseDTO;
import com.kiert.backend.entity.EventoAuditoria;
import com.kiert.backend.entity.TipoCodigo;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.repository.UsuarioRepository;
import com.kiert.backend.security.JwtService;
import com.kiert.backend.service.AuditoriaService;
import com.kiert.backend.service.CodigoVerificacionService;
import com.kiert.backend.service.EmailService;
import com.kiert.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

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

    // ============================================================
    // REGISTRO
    // ============================================================
    @Transactional
    public void registrar(RegisterRequestDTO datos) {
        log.info("Registrando usuario: {}", datos.email());

        // Rate limiting por email (máx 3 registros por hora por email)
        rateLimitService.verificar(
                "registro:email:" + datos.email(),
                3,
                Duration.ofHours(1)
        );

        // Validar duplicados
        if (usuarioRepository.existsByEmail(datos.email())) {
            auditoriaService.registrar(
                    null,
                    datos.email(),
                    EventoAuditoria.REGISTRO,
                    "Intento de registro con email ya existente",
                    false
            );
            throw new BadRequestException("El email ya esta registrado");
        }

        if (usuarioRepository.existsByNombreUsuario(datos.nombreUsuario())) {
            auditoriaService.registrar(
                    null,
                    datos.email(),
                    EventoAuditoria.REGISTRO,
                    "Intento de registro con nombre de usuario ya existente",
                    false
            );
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
                "Usuario registrado. Codigo de verificacion enviado al email.",
                true
        );

        log.info("Usuario registrado (pendiente verificacion): {}", datos.email());
    }

    // ============================================================
    // VERIFICAR CUENTA
    // ============================================================
    @Transactional
    public void verificarCuenta(String email, String codigo) {
        log.info("Verificando cuenta: {}", email);

        // Rate limiting: máx 5 intentos cada 15 min por email
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
                    "Intento fallido: codigo incorrecto o expirado",
                    false
            );
            throw e;
        }

        // Éxito: resetear rate limit
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
    // REENVIAR CODIGO DE VERIFICACION
    // ============================================================
    @Transactional
    public void reenviarCodigoVerificacion(String email) {
        log.info("Reenviando codigo de verificacion: {}", email);

        // Rate limiting: máx 3 reenvíos por hora por email
        rateLimitService.verificar(
                "reenvio:email:" + email,
                3,
                Duration.ofHours(1)
        );

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadRequestException("Esta cuenta ya esta activa");
        }

        String codigo = codigoVerificacionService.generarCodigo(
                email, TipoCodigo.VERIFICACION_CUENTA);

        emailService.enviarCodigoVerificacion(email, usuario.getNombreUsuario(), codigo);

        auditoriaService.registrar(
                usuario.getId(),
                email,
                EventoAuditoria.REENVIO_CODIGO,
                "Codigo de verificacion reenviado al email",
                true
        );
    }

    // ============================================================
    // LOGIN
    // ============================================================
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO datos) {
        log.info("Login para usuario: {}", datos.email());

        // 🔒 RATE LIMITING: máx 5 intentos cada 15 min por email
        rateLimitService.verificar(
                "login:email:" + datos.email(),
                5,
                Duration.ofMinutes(15)
        );

        Usuario usuario = usuarioRepository.findByEmail(datos.email())
                .orElse(null);

        if (usuario == null) {
            auditoriaService.registrar(
                    null,
                    datos.email(),
                    EventoAuditoria.LOGIN_FALLIDO,
                    "Intento de login con email no registrado",
                    false
            );
            throw new BadRequestException("Credenciales invalidas");
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            auditoriaService.registrar(
                    usuario.getId(),
                    datos.email(),
                    EventoAuditoria.CUENTA_NO_VERIFICADA,
                    "Intento de login con cuenta sin verificar",
                    false
            );
            throw new BadRequestException(
                    "Debes verificar tu cuenta primero. Revisa tu correo (incluida la carpeta de Spam)."
            );
        }

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
                    "Contrasena incorrecta",
                    false
            );
            throw new BadRequestException("Credenciales invalidas");
        }

        // ✅ LOGIN EXITOSO: resetear rate limit
        rateLimitService.resetear("login:email:" + datos.email());

        // Marcar en línea
        usuarioService.marcarEnLinea(usuario.getId());
        log.info("Usuario {} marcado como EN LINEA tras login", usuario.getId());

        auditoriaService.registrar(
                usuario.getId(),
                datos.email(),
                EventoAuditoria.LOGIN,
                "Inicio de sesion correcto",
                true
        );

        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail());
        return new AuthResponseDTO(token, aDTO(usuario));
    }

    // ============================================================
    // LOGOUT (con blacklist de JWT)
    // ============================================================
    @Transactional
    public void logout(Long usuarioId, String token) {
        log.info("Cerrando sesion para usuario: {}", usuarioId);

        if (usuarioId != null) {
            usuarioService.marcarDesconectado(usuarioId);

            // 🔒 AÑADIR TOKEN A BLACKLIST
            if (token != null && !token.isBlank()) {
                try {
                    Instant expiracion = jwtService.obtenerExpiracion(token);
                    tokenBlacklistService.invalidar(token, expiracion);
                } catch (Exception e) {
                    log.error("⚠️ Error invalidando token en logout: {}", e.getMessage());
                }
            }

            auditoriaService.registrar(
                    usuarioId,
                    null,
                    EventoAuditoria.LOGOUT,
                    "Cierre de sesion",
                    true
            );
        }
    }

    // ============================================================
    // SOLICITAR RECUPERACION
    // ============================================================
    @Transactional
    public void solicitarRecuperacion(String email) {
        log.info("Solicitando recuperacion para: {}", email);

        // Rate limiting: máx 3 solicitudes por hora por email
        rateLimitService.verificar(
                "recuperacion:email:" + email,
                3,
                Duration.ofHours(1)
        );

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    auditoriaService.registrar(
                            null,
                            email,
                            EventoAuditoria.SOLICITUD_RECUPERACION,
                            "Solicitud de recuperacion para email no registrado",
                            false
                    );
                    return new BadRequestException("No existe una cuenta con ese correo");
                });

        String codigo = codigoVerificacionService.generarCodigo(
                email, TipoCodigo.RECUPERACION_PASSWORD);

        emailService.enviarCodigoRecuperacion(email, usuario.getNombreUsuario(), codigo);

        auditoriaService.registrar(
                usuario.getId(),
                email,
                EventoAuditoria.SOLICITUD_RECUPERACION,
                "Codigo de recuperacion enviado al email",
                true
        );

        log.info("Codigo de recuperacion enviado a: {}", email);
    }

    // ============================================================
    // RESET PASSWORD CON CODIGO
    // ============================================================
    @Transactional
    public void restablecerPasswordConCodigo(String email, String codigo, String nuevaPassword) {
        log.info("Restableciendo contrasena para: {}", email);

        // Rate limiting: máx 5 intentos cada 15 min
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
                    "Intento fallido: codigo incorrecto o expirado",
                    false
            );
            throw e;
        }

        // ✅ Éxito: resetear rate limit
        rateLimitService.resetear("reset:email:" + email);

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setFechaUltimoCambioPassword(Instant.now());
        usuarioRepository.save(usuario);

        // 🔒 Invalidar TODOS los tokens del usuario (forzar re-login)
        // (Esto requiere que guardes los tokens por usuario o uses un "token version")
        // Por ahora, no lo hacemos porque JwtService no lo soporta.

        auditoriaService.registrar(
                usuario.getId(),
                email,
                EventoAuditoria.CAMBIO_PASSWORD,
                "Contrasena restablecida correctamente con codigo",
                true
        );

        log.info("Contrasena restablecida para {}", email);
    }

    // ============================================================
    // DTO HELPER
    // ============================================================
    private UsuarioDTO aDTO(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl()
        );
    }
}