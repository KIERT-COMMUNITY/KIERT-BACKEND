package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearComentarioDTO(
        @NotBlank(message = "El contenido del comentario es obligatorio")
        String contenido
) {}