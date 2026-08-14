package com.kiert.backend.dto;

import java.time.Instant;

public record MensajeChatDTO(
        Long id,
        Long emisorId,
        String contenido,
        Instant fechaEnvio,
        boolean propio
) {}