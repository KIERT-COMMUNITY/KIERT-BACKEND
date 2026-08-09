package com.kiert.backend.dto;

public record AdjuntoDTO(
        Long id,
        String tipo,   // "archivo" | "link"
        String nombre,
        String url,
        Integer pesoKb
) {}
