package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.PasswordResetToken;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.CredencialesInvalidasException;
import com.kiert.backend.exception.RecursoDuplicadoException;
import com.kiert.backend.exception.TokenInvalidoException;
import com.kiert.backend.repository.PasswordResetTokenRepository;
import com.kiert.backend.repository.UsuarioRepository;
import com.kiert.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Espejo del backend que necesita auth.service.ts: login, registro,
// recuperar/restablecer contraseña.
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int HORAS_VIGENCIA_TOKEN_RESET = 2;

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Transactional
    public AuthResponseDTO registrar(RegisterRequestDTO datos) {
        if (usuarioRepository.existsByEmail(datos.email())) {
            throw new RecursoDuplicadoException("Ese correo ya está registrado.");
        }
        if (usuarioRepository.existsByNombreUsuario(datos.nombreUsuario())) {
            throw new RecursoDuplicadoException("Ese nombre de usuario ya está en uso.");
        }

        Usuario usuario = Usuario.builder()
                .nombreUsuario(datos.nombreUsuario())
                .email(datos.email())
                .passwordHash(passwordEncoder.encode(datos.password()))
                .build();

        usuario = usuarioRepository.save(usuario);
        return construirRespuestaAuth(usuario);
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO datos) {
        Usuario usuario = usuarioRepository.findByEmail(datos.email())
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos."));

        if (!passwordEncoder.matches(datos.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos.");
        }

        return construirRespuestaAuth(usuario);
    }

    @Transactional
    public MensajeSimpleDTO solicitarRecuperacion(String email) {
        // Por seguridad, SIEMPRE devolvemos el mismo mensaje exista o no el correo
        // (igual que indica el comentario en forgot-password.component.ts)
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            PasswordResetToken token = PasswordResetToken.builder()
                    .usuario(usuario)
                    .fechaExpiracion(Instant.now().plus(HORAS_VIGENCIA_TOKEN_RESET, ChronoUnit.HOURS))
                    .build();
            tokenRepository.save(token);
            emailService.enviarCorreoRecuperacion(usuario.getEmail(), token.getToken());
        });

        return new MensajeSimpleDTO("Si el correo existe, te enviamos un enlace para restablecer tu contraseña.");
    }

    @Transactional
    public MensajeSimpleDTO restablecerContrasena(String token, String nuevaContrasena) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenInvalidoException("El enlace expiró o ya fue usado."));

        if (!resetToken.estaVigente()) {
            throw new TokenInvalidoException("El enlace expiró o ya fue usado.");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        tokenRepository.save(resetToken);

        return new MensajeSimpleDTO("Contraseña actualizada correctamente.");
    }

    private AuthResponseDTO construirRespuestaAuth(Usuario usuario) {
        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail());
        UsuarioDTO usuarioDTO = new UsuarioDTO(
                usuario.getId(), usuario.getNombreUsuario(), usuario.getEmail(), usuario.getFotoPerfilUrl());
        return new AuthResponseDTO(token, usuarioDTO);
    }
}
