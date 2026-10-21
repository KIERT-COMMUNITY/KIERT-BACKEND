// src/main/java/com/kiert/backend/security/UsuarioPrincipal.java
package com.kiert.backend.security;

import com.kiert.backend.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class UsuarioPrincipal implements UserDetails {

    private final Usuario usuario;

    public UsuarioPrincipal(Usuario usuario) {
        this.usuario = usuario;
    }

    public Long getId() {
        return usuario.getId();
    }

    public String getRol() {
        return usuario.getRol() != null ? usuario.getRol() : "USER";
    }

    /**
     * Authorities basadas en el rol REAL del usuario.
     *
     * - Si el rol es USER: solo ROLE_USER
     * - Si el rol es ADMIN/CEO/MODERADOR: el rol específico + ROLE_USER
     *
     * Nota: no incluimos ROLE_USER automáticamente para admins,
     * para que las rutas con .hasRole("USER") los excluyan.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        String rol = getRol();

        authorities.add(new SimpleGrantedAuthority("ROLE_" + rol));

        // Si es USER, no duplicamos
        if (!"USER".equalsIgnoreCase(rol)) {
            // Admins NO tienen ROLE_USER (así /api/chat/** los excluye)
            // authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return usuario.getBloqueadoHasta() == null
                || usuario.getBloqueadoHasta().isBefore(java.time.Instant.now());
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(usuario.getActivo())
                && !Boolean.TRUE.equals(usuario.getEliminado());
    }
}