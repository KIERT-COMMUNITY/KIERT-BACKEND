package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearComentarioDTO(
        @NotBlank @Size(min = 2) String contenido
) {}
