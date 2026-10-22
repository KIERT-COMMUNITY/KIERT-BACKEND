package com.kiert.backend.features.autor.dto;

public record AutorResumenDTO(
        Long id,
        String nombreUsuario,
        String fotoPerfilUrl,
        String marcoId
) {}