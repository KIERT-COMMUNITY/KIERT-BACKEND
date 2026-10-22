package com.kiert.backend.features.usuario.dto;

public record UsuarioDTO(
        Long id,
        String nombreUsuario,
        String email,
        String fotoPerfilUrl,
        String bio
) {}