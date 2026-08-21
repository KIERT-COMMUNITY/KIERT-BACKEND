package com.kiert.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual {

    public Long id() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                System.err.println("❌ UsuarioActual: No hay autenticación");
                return null;
            }

            Object principal = auth.getPrincipal();

            if (principal == null) {
                System.err.println("❌ UsuarioActual: principal es null");
                return null;
            }

            if (principal instanceof UsuarioPrincipal) {
                Long id = ((UsuarioPrincipal) principal).getId();
                System.out.println("✅ UsuarioActual: ID obtenido de UsuarioPrincipal = " + id);
                return id;
            }

            // Si principal es String (email)
            if (principal instanceof String) {
                System.err.println("❌ UsuarioActual: principal es String, no se puede obtener ID: " + principal);
                return null;
            }

            System.err.println("❌ UsuarioActual: No se pudo obtener ID, principal tipo: " + principal.getClass().getName());
            return null;
        } catch (Exception e) {
            System.err.println("❌ UsuarioActual: Error al obtener ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}