package com.kiert.backend.features.usuario.dto;

public record UsuarioDisponibleDTO(
        Long id,
        String nombreUsuario,
        String fotoPerfilUrl
) {}