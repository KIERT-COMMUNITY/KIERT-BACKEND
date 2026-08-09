package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Campos de texto que llegan como multipart/form-data junto a los archivos
// (ver create-post.component.ts: FormData con titulo/categoria/descripcion/link/archivos)
public record CrearPostDTO(
        @NotBlank @Size(min = 6, max = 120) String titulo,
        @NotBlank String categoria,
        @NotBlank @Size(min = 20) String descripcion,
        String link
) {}
