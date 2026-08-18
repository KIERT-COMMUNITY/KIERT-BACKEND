package com.kiert.backend.dto;

public record MarcoDTO(
        String id,
        String nombre,
        String urlImagen,
        String tipo,
        Double precio,
        boolean gratis
) {}