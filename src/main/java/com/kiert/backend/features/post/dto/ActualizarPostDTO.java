package com.kiert.backend.features.post.dto;

import jakarta.validation.constraints.Size;

public record ActualizarPostDTO(
        @Size(max = 120, message = "El título no puede tener más de 120 caracteres")
        String titulo,
        String descripcion,
        String categoria
) {}