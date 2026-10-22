package com.kiert.backend.features.comentarios.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearComentarioDTO(
        @NotBlank(message = "El contenido es obligatorio")
        String contenido,

        String urlImagen
) {}