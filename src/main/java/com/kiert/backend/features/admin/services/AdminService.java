// src/main/java/com/kiert/backend/features/admin/services/AdminService.java
package com.kiert.backend.features.admin.services;

import com.kiert.backend.features.admin.dtos.AdminDTO;
import com.kiert.backend.features.admin.dtos.request.AdminLoginRequestDTO;
import com.kiert.backend.features.admin.dtos.response.AdminLoginResponseDTO;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
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

    @Transactional(readOnly = true)
    public AdminLoginResponseDTO login(AdminLoginRequestDTO req) {
        log.info("[ADMIN] Intento de login: {}", req.email());

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(req.email());

        if (usuarioOpt.isEmpty()) {
            passwordEncoder.matches(
                    req.password(),
                    "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
            );
            log.warn("[ADMIN] Email no encontrado: {}", req.email());
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        Usuario usuario = usuarioOpt.get();

        if (!passwordEncoder.matches(req.password(), usuario.getPasswordHash())) {
            log.warn("[ADMIN] Contrasena incorrecta para: {}", req.email());
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("[ADMIN] Cuenta inactiva: {}", req.email());
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        if (Boolean.TRUE.equals(usuario.getEliminado())) {
            log.warn("[ADMIN] Cuenta eliminada: {}", req.email());
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        if (usuario.getBloqueadoHasta() != null
                && usuario.getBloqueadoHasta().isAfter(java.time.Instant.now())) {
            log.warn("[ADMIN] Cuenta bloqueada: {}", req.email());
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        String rol = usuario.getRol() != null ? usuario.getRol() : "USER";
        if (!ROLES_ADMIN.contains(rol)) {
            log.warn("[ADMIN] Sin permisos: {} (rol={})", req.email(), rol);
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        int tokenVersion = usuario.getTokenVersion() != null ? usuario.getTokenVersion() : 0;
        String token = jwtService.generarTokenConRol(
                usuario.getId(),
                usuario.getEmail(),
                rol,
                tokenVersion
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