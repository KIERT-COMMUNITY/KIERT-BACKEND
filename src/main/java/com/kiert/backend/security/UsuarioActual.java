// src/main/java/com/kiert/backend/security/UsuarioActual.java
package com.kiert.backend.security;

import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioActual {

    private final UsuarioRepository usuarioRepository;

    /**
     * Devuelve el ID del usuario autenticado, o null si no hay sesión.
     *
     * Solo confía en:
     * - UsuarioPrincipal (el UserDetails custom, que es lo que inyecta JwtAuthFilter)
     *
     * No confía en:
     * - Strings sueltos
     * - UserDetails genéricos que no sean UsuarioPrincipal
     */
    public Long id() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return null;
            }

            Object principal = auth.getPrincipal();

            // Caso principal y ÚNICO válido: UsuarioPrincipal
            if (principal instanceof UsuarioPrincipal up) {
                return up.getId();
            }

            log.warn("UsuarioActual: principal no reconocido ({})",
                    principal != null ? principal.getClass().getName() : "null");
            return null;

        } catch (Exception e) {
            log.error("UsuarioActual error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Devuelve el email del usuario autenticado, o null.
     */
    public String email() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;

            Object principal = auth.getPrincipal();
            if (principal instanceof UsuarioPrincipal up) {
                return up.getUsername();
            }
            return null;
        } catch (Exception e) {
            log.error("UsuarioActual error: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Devuelve el rol del usuario autenticado, o null.
     */
    public String rol() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;

            Object principal = auth.getPrincipal();
            if (principal instanceof UsuarioPrincipal up) {
                return up.getRol();
            }
            return null;
        } catch (Exception e) {
            log.error("UsuarioActual error: {}", e.getMessage(), e);
            return null;
        }
    }
}