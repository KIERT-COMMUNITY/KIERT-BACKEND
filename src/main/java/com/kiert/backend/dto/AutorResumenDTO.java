package com.kiert.backend.dto;

// Autor "resumido" que se anida dentro de Post y Comentario (ver post.model.ts)
public record AutorResumenDTO(
        Long id,
        String nombreUsuario,
        String fotoPerfilUrl
) {}
