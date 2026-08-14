package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearPostDTO(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 120, message = "El título no puede tener más de 120 caracteres")
        String titulo,

        @NotBlank(message = "La categoría es obligatoria")
        String categoria,

        @NotBlank(message = "La descripción es obligatoria")
        String descripcion,

        String link
) {}