// src/main/java/com/kiert/backend/features/chatGlobal/dto/EnviarMensajeGlobalDTO.java
package com.kiert.backend.features.chatGlobal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeGlobalDTO(

        @NotBlank(message = "El contenido no puede estar vacío")
        @Size(max = 500, message = "El contenido no puede superar los 500 caracteres")
        String contenido,

        /**
         * ID del mensaje al que se está respondiendo.
         * null si es un mensaje normal (no es respuesta).
         */
        Long respuestaAId

) {}