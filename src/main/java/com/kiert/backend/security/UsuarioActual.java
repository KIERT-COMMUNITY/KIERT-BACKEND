// src/main/java/com/kiert/backend/security/UsuarioActual.java
package com.kiert.backend.security;

import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioActual {

    private final UsuarioRepository usuarioRepository;

    public Long id() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                log.debug("⚠️ UsuarioActual: sin autenticación");
                return null;
            }

            Object principal = auth.getPrincipal();
            if (principal == null) {
                log.warn("⚠️ UsuarioActual: principal null");
                return null;
            }

            // ✅ CASO PRINCIPAL: UsuarioPrincipal (tu UserDetails custom)
            if (principal instanceof UsuarioPrincipal up) {
                log.debug("✅ UsuarioActual (UsuarioPrincipal): {}", up.getId());
                return up.getId();
            }

            // Fallback 1: String (email o ID)
            if (principal instanceof String s) {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException ignored) {
                    return usuarioRepository.findByEmail(s)
                            .map(u -> u.getId())
                            .orElse(null);
                }
            }

            // Fallback 2: UserDetails estándar (por si acaso)
            if (principal instanceof UserDetails ud) {
                String username = ud.getUsername();
                try {
                    return Long.parseLong(username);
                } catch (NumberFormatException ignored) {
                    return usuarioRepository.findByEmail(username)
                            .map(u -> u.getId())
                            .orElse(null);
                }
            }

            log.warn("⚠️ UsuarioActual: tipo no soportado: {}",
                    principal.getClass().getName());
            return null;

        } catch (Exception e) {
            log.error("❌ UsuarioActual error: {}", e.getMessage(), e);
            return null;
        }
    }
}