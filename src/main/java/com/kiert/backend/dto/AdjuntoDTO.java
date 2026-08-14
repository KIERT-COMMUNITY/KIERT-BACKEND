package com.kiert.backend.dto;

public record AdjuntoDTO(
        Long id,
        String tipo,
        String nombre,
        String url,
        Integer pesoKb
) {}