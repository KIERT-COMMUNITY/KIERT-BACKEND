package com.kiert.backend.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ============================================================
    // REGISTRO -> crea usuario INACTIVO y envia codigo
    // ============================================================
    @Transactional
    public void registrar(RegisterRequestDTO datos) {
        log.info("Registrando usuario: {}", datos.email());

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

        // Crear usuario INACTIVO
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

        // Generar codigo y enviar
        String codigo = codigoVerificacionService.generarCodigo(
                datos.email(), TipoCodigo.VERIFICACION_CUENTA);

        emailService.enviarCodigoVerificacion(
                datos.email(), datos.nombreUsuario(), codigo);

        // AUDITORIA
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
    // VERIFICAR CUENTA -> activa + envia bienvenida
    // ============================================================
    @Transactional
    public void verificarCuenta(String email, String codigo) {
        log.info("Verificando cuenta: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(usuario.getActivo())) {
            log.info("La cuenta ya estaba activa: {}", email);
            return;
        }

        // Validar codigo (lanza excepcion si falla)
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

        // Activar cuenta
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setFechaVerificacionEmail(Instant.now());
        usuarioRepository.save(usuario);

        // Enviar bienvenida
        emailService.enviarCorreoBienvenida(email, usuario.getNombreUsuario());

        // AUDITORIA
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

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadRequestException("Esta cuenta ya esta activa");
        }

        String codigo = codigoVerificacionService.generarCodigo(
                email, TipoCodigo.VERIFICACION_CUENTA);

        emailService.enviarCodigoVerificacion(email, usuario.getNombreUsuario(), codigo);

        // AUDITORIA
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

        // Buscar usuario
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

        // Verificar si la cuenta esta activa
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

        // Autenticar
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

        // Marcar en linea
        usuarioService.marcarEnLinea(usuario.getId());
        log.info("Usuario {} marcado como EN LINEA tras login", usuario.getId());

        // AUDITORIA
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
    // LOGOUT
    // ============================================================
    @Transactional
    public void logout(Long usuarioId) {
        log.info("Cerrando sesion para usuario: {}", usuarioId);

        if (usuarioId != null) {
            usuarioService.marcarDesconectado(usuarioId);

            // AUDITORIA
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
    // SOLICITAR RECUPERACION -> envia codigo
    // ============================================================
    @Transactional
    public void solicitarRecuperacion(String email) {
        log.info("Solicitando recuperacion para: {}", email);

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

        // AUDITORIA
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

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        // Validar codigo
        try {
            codigoVerificacionService.validarYConsumir(
                    email, codigo, TipoCodigo.RECUPERACION_PASSWORD);
        } catch (Exception e) {
            // AUDITORIA: intento fallido
            auditoriaService.registrar(
                    usuario.getId(),
                    email,
                    EventoAuditoria.CAMBIO_PASSWORD,
                    "Intento fallido: codigo incorrecto o expirado",
                    false
            );
            throw e;
        }

        // Cambiar contrasena
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setFechaUltimoCambioPassword(Instant.now());
        usuarioRepository.save(usuario);

        // AUDITORIA: cambio exitoso
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