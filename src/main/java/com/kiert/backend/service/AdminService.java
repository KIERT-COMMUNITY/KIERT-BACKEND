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

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // ✅ Roles que pueden acceder al panel admin
    private static final List<String> ROLES_ADMIN = List.of("CEO", "ADMIN", "MODERADOR");

    @Transactional(readOnly = true)
    public AdminLoginResponseDTO login(AdminLoginRequestDTO req) {
        log.info("[ADMIN] Intento de login: {}", req.email());

        // 1. Buscar usuario
        Usuario usuario = usuarioRepository.findByEmail(req.email())
                .orElseThrow(() -> {
                    log.warn("[ADMIN] Email no encontrado: {}", req.email());
                    return new BadCredentialsException("Credenciales incorrectas");
                });

        // 2. Verificar contraseña
        if (!passwordEncoder.matches(req.password(), usuario.getPasswordHash())) {
            log.warn("[ADMIN] Password incorrecta para: {}", req.email());
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        // 3. Verificar cuenta activa
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("[ADMIN] Cuenta inactiva: {}", req.email());
            throw new BadCredentialsException("Cuenta inactiva. Verifica tu correo.");
        }

        // 4. ✅ Verificar rol de admin
        String rol = usuario.getRol() != null ? usuario.getRol() : "USER";
        if (!ROLES_ADMIN.contains(rol)) {
            log.warn("[ADMIN] Usuario sin permisos: {} (rol={})", req.email(), rol);
            throw new BadCredentialsException("No tienes permisos de administrador");
        }

        // 5. ✅ Generar token — CORREGIDO: pasar (id, email)
        String token = jwtService.generarToken(
                usuario.getId(),
                usuario.getEmail()
        );

        log.info("[ADMIN] ✅ Login exitoso: {} (rol={})", req.email(), rol);

        return new AdminLoginResponseDTO(
                token,
                new AdminDTO(
                        usuario.getId(),
                        usuario.getEmail(),
                        usuario.getNombreUsuario(),
                        rol
                ),
                86400L // 24h
        );
    }
}