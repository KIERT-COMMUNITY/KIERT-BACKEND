// src/main/java/com/kiert/backend/dto/MensajeGlobalDTO.java
package com.kiert.backend.features.chatGlobal.dto;

import java.time.Instant;

public record MensajeGlobalDTO(
        Long id,
        Long autorId,
        String autorNombreUsuario,
        String autorFotoPerfilUrl,
        String contenido,
        Instant fechaEnvio
) {}