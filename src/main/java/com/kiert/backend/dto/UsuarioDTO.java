package com.kiert.backend.dto;

public record UsuarioDTO(
        Long id,
        String nombreUsuario,
        String email,
        String fotoPerfilUrl
) {}