package com.kiert.backend.dto;

public record UsuarioDisponibleDTO(
        Long id,
        String nombreUsuario,
        String fotoPerfilUrl
) {}