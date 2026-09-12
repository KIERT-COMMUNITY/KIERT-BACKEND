package com.kiert.backend.service;

import com.kiert.backend.dto.response.AuthResponseDTO;
import com.kiert.backend.dto.request.LoginRequestDTO;
import com.kiert.backend.dto.request.RegisterRequestDTO;
import com.kiert.backend.dto.UsuarioDTO;
import com.kiert.backend.entity.PasswordResetToken;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.PasswordResetTokenRepository;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService; // ✅ INYECTAR EmailService

    // ========== REGISTRO ==========
    @Transactional
    public AuthResponseDTO registrar(RegisterRequestDTO datos) {
        log.info("📝 Registrando usuario: {}", datos.email());

        if (usuarioRepository.existsByEmail(datos.email())) {
            throw new BadRequestException("El email ya está registrado");
        }

        if (usuarioRepository.existsByNombreUsuario(datos.nombreUsuario())) {
            throw new BadRequestException("El nombre de usuario ya está en uso");
        }

        Usuario usuario = Usuario.builder()
                .nombreUsuario(datos.nombreUsuario())
                .email(datos.email())
                .passwordHash(passwordEncoder.encode(datos.password()))
                .fechaCreacion(Instant.now())
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("✅ Usuario registrado con ID: {}", usuario.getId());

        // ✅ Enviar correo de bienvenida
        try {
            emailService.enviarCorreoBienvenida(usuario.getEmail(), usuario.getNombreUsuario());
        } catch (Exception e) {
            log.warn("⚠️ No se pudo enviar correo de bienvenida: {}", e.getMessage());
        }

        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail());
        return new AuthResponseDTO(token, aDTO(usuario));
    }

    // ========== LOGIN ==========
    public AuthResponseDTO login(LoginRequestDTO datos) {
        log.info("🔑 Login para usuario: {}", datos.email());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(datos.email(), datos.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Usuario usuario = usuarioRepository.findByEmail(datos.email())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail());
        return new AuthResponseDTO(token, aDTO(usuario));
    }

    // ========== SOLICITAR RECUPERACIÓN DE CONTRASEÑA ==========
    @Transactional
    public void solicitarRecuperacion(String email) {
        log.info("📧 Solicitando recuperación para: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con ese email"));

        // ✅ Eliminar tokens anteriores del mismo usuario
        passwordResetTokenRepository.deleteAll(
                passwordResetTokenRepository.findByUsuarioId(usuario.getId())
        );

        String token = UUID.randomUUID().toString();
        Instant fechaExpiracion = Instant.now().plusSeconds(3600); // 1 hora

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .usuario(usuario)
                .fechaExpiracion(fechaExpiracion)
                .usado(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.info("✅ Token de recuperación generado para: {}", email);

        // ✅ Enviar correo con el token
        try {
            emailService.enviarCorreoRecuperacion(email, token);
            log.info("✅ Correo de recuperación enviado a: {}", email);
        } catch (Exception e) {
            log.error("❌ Error al enviar correo de recuperación: {}", e.getMessage());
            throw new BadRequestException("Error al enviar el correo de recuperación. Intenta nuevamente.");
        }
    }

    // ========== RESTABLECER CONTRASEÑA ==========
    @Transactional
    public void restablecerContrasena(String token, String nuevaContrasena) {
        log.info("🔑 Restableciendo contraseña");

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Token inválido o expirado"));

        if (resetToken.isExpirado()) {
            throw new BadRequestException("El token ha expirado. Solicita uno nuevo.");
        }

        if (resetToken.isUsado()) {
            throw new BadRequestException("El token ya ha sido usado");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("✅ Contraseña restablecida para usuario: {}", usuario.getEmail());
    }

    // ========== VALIDAR TOKEN ==========
    public boolean validarToken(String token) {
        log.info("🔍 Validando token");
        return passwordResetTokenRepository.findByToken(token)
                .map(resetToken -> !resetToken.isExpirado() && !resetToken.isUsado())
                .orElse(false);
    }

    // ========== DTO HELPER ==========
    private UsuarioDTO aDTO(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl()
        );
    }
}