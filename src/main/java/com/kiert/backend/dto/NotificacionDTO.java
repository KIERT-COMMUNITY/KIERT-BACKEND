package com.kiert.backend.dto;

import java.time.Instant;

public record NotificacionDTO(
        Long id,
        String tipo,
        String mensaje,
        boolean leida,
        Instant fecha,
        Long usuarioId,
        String usuarioNombre,
        String usuarioFoto,
        Long postId,
        Long comentarioId,
        Long respuestaId,
        String url,
        Long grupoId  // ✅ AGREGAR ESTE CAMPO
) {}