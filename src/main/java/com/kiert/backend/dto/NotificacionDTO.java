// src/main/java/com/kiert/backend/dto/NotificacionDTO.java
package com.kiert.backend.dto;

import java.time.Instant;

public record NotificacionDTO(
        Long id,
        String tipo,
        String mensaje,
        Boolean leida,
        Instant fecha,
        Long usuarioId,
        String usuarioNombre,
        String usuarioFoto,
        Long postId,
        Long comentarioId,
        Long respuestaId,
        String url
) {}