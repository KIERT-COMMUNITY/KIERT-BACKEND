package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearPostDTO(
        @NotBlank(message = "El título es obligatorio")
        @Size(min = 6, max = 120, message = "El título debe tener entre 6 y 120 caracteres")
        String titulo,

        @NotBlank(message = "La categoría es obligatoria")
        @Size(min = 2, max = 50, message = "La categoría debe tener entre 2 y 50 caracteres")
        String categoria,  // ✅ String, no enum

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 20, message = "La descripción debe tener al menos 20 caracteres")
        String descripcion,

        String link
) {}