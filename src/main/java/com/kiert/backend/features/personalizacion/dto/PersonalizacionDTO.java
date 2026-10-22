package com.kiert.backend.features.personalizacion.dto;

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