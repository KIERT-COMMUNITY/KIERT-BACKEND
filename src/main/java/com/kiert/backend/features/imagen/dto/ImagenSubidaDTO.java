package com.kiert.backend.features.imagen.dto;

public record ImagenSubidaDTO(
        String url,
        String publicId,
        String formato,
        Integer pesoKb,
        Integer ancho,
        Integer alto
) {}