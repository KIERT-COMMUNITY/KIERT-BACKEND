package com.kiert.backend.dto;

// Espejo de "User" en user.model.ts - nunca incluye el password.
public record UsuarioDTO(
        Long id,
        String nombreUsuario,
        String email,
        String fotoPerfilUrl
) {}
