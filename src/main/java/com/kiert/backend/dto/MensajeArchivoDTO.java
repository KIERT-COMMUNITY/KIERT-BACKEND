package com.kiert.backend.dto;

public record MensajeArchivoDTO(
        Long id,
        String nombre,
        String url,
        String tipo,
        Integer pesoKb,
        boolean esSensible
) {}