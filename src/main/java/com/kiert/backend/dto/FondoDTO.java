package com.kiert.backend.dto;

public record FondoDTO(
        String id,
        String nombre,
        String urlImagen,
        String tipo,
        String gradiente,
        Double precio,
        boolean gratis
) {}