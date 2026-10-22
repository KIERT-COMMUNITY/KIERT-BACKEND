package com.kiert.backend.features.mensajes.dto;

import jakarta.validation.constraints.NotBlank;

public record EnviarMensajeDTO(
        @NotBlank(message = "El contenido es obligatorio")
        String contenido
) {}