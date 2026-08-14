package com.kiert.backend.dto;

import java.time.Instant;

public record ComentarioDTO(
        Long id,
        AutorResumenDTO autor,
        String contenido,
        Instant fechaCreacion
) {}