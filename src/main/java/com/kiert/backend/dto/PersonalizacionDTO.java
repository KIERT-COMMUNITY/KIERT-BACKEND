package com.kiert.backend.dto;

public record PersonalizacionDTO(
        Long id,
        Long usuarioId,
        String temaId,
        String marcoId,
        String fondoId,
        String fotoPerfilUrl,
        String fotoPortadaUrl,
        String marcoPersonalizadoUrl
) {}