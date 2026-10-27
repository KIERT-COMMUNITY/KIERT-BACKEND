package com.kiert.backend.features.bienvenida.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarBienvenidaDTO(
        @NotBlank(message = "El título no puede estar vacío")
        @Size(max = 200)
        String titulo,

        @NotBlank(message = "El contenido no puede estar vacío")
        @Size(max = 5000)
        String contenido
) {}