package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarFotoPerfilDTO(
        @NotBlank(message = "La URL de la foto es obligatoria")
        String urlFoto
) {}