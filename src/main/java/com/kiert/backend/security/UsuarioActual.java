package com.kiert.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Pequeño helper para que los controllers obtengan el id del usuario autenticado
// (el que armó el JwtAuthFilter a partir del token) sin repetir el casteo en cada uno.
@Component
public class UsuarioActual {

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UsuarioPrincipal principal = (UsuarioPrincipal) auth.getPrincipal();
        return principal.getId();
    }
}
