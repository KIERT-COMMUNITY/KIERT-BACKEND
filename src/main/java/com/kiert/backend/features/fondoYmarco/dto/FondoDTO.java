package com.kiert.backend.features.fondoYmarco.dto;

public record FondoDTO(
        String id,
        String nombre,
        String urlImagen,
        String tipo,
        String gradiente,
        Double precio,
        boolean gratis
) {}