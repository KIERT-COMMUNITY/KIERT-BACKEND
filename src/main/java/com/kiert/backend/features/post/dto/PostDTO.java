// src/main/java/com/kiert/backend/features/post/dto/PostDTO.java
package com.kiert.backend.features.post.dto;

import com.kiert.backend.features.autor.dto.AutorResumenDTO;
import com.kiert.backend.features.bloqueo.dto.AdjuntoDTO;

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
        Instant fechaCreacion,
        boolean esAnonimo
) {}