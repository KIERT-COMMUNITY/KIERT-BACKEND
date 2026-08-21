package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record EnviarMensajeDTO(
        @NotBlank(message = "El contenido es obligatorio")
        String contenido
) {}