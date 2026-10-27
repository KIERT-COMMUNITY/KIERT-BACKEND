package com.kiert.backend.features.bienvenida.dto;

public record BienvenidaDTO(
        String titulo,
        String contenido,
        String fechaActualizacion,
        Long actualizadoPorId,
        String actualizadoPorNombre
) {}