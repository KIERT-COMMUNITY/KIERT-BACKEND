package com.kiert.backend.features.compartidos.dto;

import java.time.Instant;

public record CompartidoDTO(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String usuarioFoto,
        Long postId,
        String postTitulo,
        String tipoCompartido,
        String comentario,
        Instant fechaCreacion
) {}