// src/main/java/com/kiert/backend/features/post/dto/CrearPostDTO.java
package com.kiert.backend.features.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearPostDTO(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(min = 6, max = 120, message = "El titulo debe tener entre 6 y 120 caracteres")
        String titulo,

        @NotBlank(message = "La categoria es obligatoria")
        @Size(min = 2, max = 50, message = "La categoria debe tener entre 2 y 50 caracteres")
        String categoria,

        @NotBlank(message = "La descripcion es obligatoria")
        @Size(min = 20, message = "La descripcion debe tener al menos 20 caracteres")
        String descripcion,

        String link,

        Boolean esAnonimo
) {}