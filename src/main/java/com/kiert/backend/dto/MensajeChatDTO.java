package com.kiert.backend.dto;

import java.time.Instant;

// Espejo de Mensaje en chat.model.ts. "propio" se calcula al vuelo
// comparando emisorId con el usuario autenticado que hace la petición.
public record MensajeChatDTO(
        Long id,
        Long emisorId,
        String contenido,
        Instant fechaEnvio,
        boolean propio
) {}
