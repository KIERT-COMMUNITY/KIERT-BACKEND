package com.kiert.backend.features.chatYArchivo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeGlobalUsuarioDTO(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 2000, message = "El mensaje es demasiado largo")
        String contenido
) {}