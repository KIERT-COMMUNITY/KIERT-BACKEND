package com.kiert.backend.dto;

import java.time.Instant;
import java.util.List;

public record PostDTO(
        Long id,
        AutorResumenDTO autor,
        String titulo,
        String descripcion,
        String categoria,
        List<AdjuntoDTO> adjuntos,
        long totalComentarios,
        Instant fechaCreacion
) {}