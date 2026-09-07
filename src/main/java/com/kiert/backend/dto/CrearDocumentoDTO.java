package com.kiert.backend.dto;

public record CrearDocumentoDTO(
        String titulo,
        String descripcion,
        String categoria,
        String categoriaPersonalizada
) {}