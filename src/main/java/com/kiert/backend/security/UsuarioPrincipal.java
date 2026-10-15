// src/main/java/com/kiert/backend/security/UsuarioPrincipal.java
package com.kiert.backend.security;

import com.kiert.backend.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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

    // ✅ Expone el rol como authority de Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String rol = getRol();
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + rol),
                new SimpleGrantedAuthority("ROLE_USER") // todos son USER también
        );
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
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return Boolean.TRUE.equals(usuario.getActivo()); }
}