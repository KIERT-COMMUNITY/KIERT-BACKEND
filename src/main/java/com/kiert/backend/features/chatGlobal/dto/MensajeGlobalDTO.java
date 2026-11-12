// src/main/java/com/kiert/backend/features/chatGlobal/dto/MensajeGlobalDTO.java
package com.kiert.backend.features.chatGlobal.dto;

import java.time.Instant;

public record MensajeGlobalDTO(
        Long id,
        Long autorId,
        String autorNombreUsuario,
        String autorFotoPerfilUrl,
        String marcoId,
        String contenido,
        Instant fechaEnvio,
        Long respuestaAId
) {}