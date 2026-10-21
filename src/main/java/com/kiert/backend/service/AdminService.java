// src/main/java/com/kiert/backend/service/AdminService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.AdminDTO;
import com.kiert.backend.dto.request.AdminLoginRequestDTO;
import com.kiert.backend.dto.response.AdminLoginResponseDTO;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.repository.UsuarioRepository;
import com.kiert.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final List<String> ROLES_ADMIN = List.of("CEO", "ADMIN", "MODERADOR");

    /**
     * Login de administrador.
     *
     * MEDIDAS DE SEGURIDAD:
     * - Mensaje genérico "Credenciales incorrectas" en TODOS los casos
     *   (email no existe, contraseña incorrecta, cuenta inactiva, rol insuficiente).
     * - Ejecuta BCrypt ficticio si el usuario no existe (evita timing attacks).
     * - No revela si el email existe.
     */
    @Transactional(readOnly = true)
    public AdminLoginResponseDTO login(AdminLoginRequestDTO req) {
        log.info("[ADMIN] Intento de login: {}", req.email());

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(req.email());

        // ============================================================
        // TIMING ATTACK: si el usuario no existe, ejecutamos BCrypt
        // ficticio para simular el mismo tiempo de respuesta.
        // ============================================================
        if (usuarioOpt.isEmpty()) {
            passwordEncoder.matches(
                    req.password(),
                    "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
            );
            log.warn("[ADMIN] Email no encontrado: {}", req.email());
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        Usuario usuario = usuarioOpt.get();

        // Validación de contraseña
        if (!passwordEncoder.matches(req.password(), usuario.getPasswordHash())) {
            log.warn("[ADMIN] Contraseña incorrecta para: {}", req.email());
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        // Validación de cuenta activa
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("[ADMIN] Cuenta inactiva: {}", req.email());
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        // Validación de cuenta no eliminada
        if (Boolean.TRUE.equals(usuario.getEliminado())) {
            log.warn("[ADMIN] Cuenta eliminada: {}", req.email());
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        // Validación de cuenta no bloqueada
        if (usuario.getBloqueadoHasta() != null
                && usuario.getBloqueadoHasta().isAfter(java.time.Instant.now())) {
            log.warn("[ADMIN] Cuenta bloqueada: {}", req.email());
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        // Validación de rol
        String rol = usuario.getRol() != null ? usuario.getRol() : "USER";
        if (!ROLES_ADMIN.contains(rol)) {
            log.warn("[ADMIN] Sin permisos: {} (rol={})", req.email(), rol);
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        // Generar token con rol
        String token = jwtService.generarTokenConRol(
                usuario.getId(),
                usuario.getEmail(),
                rol
        );

        log.info("[ADMIN] Login exitoso: {} (rol={})", req.email(), rol);

        return new AdminLoginResponseDTO(
                token,
                new AdminDTO(
                        usuario.getId(),
                        usuario.getEmail(),
                        usuario.getNombreUsuario(),
                        rol
                ),
                86400L
        );
    }
}