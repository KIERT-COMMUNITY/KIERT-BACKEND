package com.kiert.backend.dto;

import java.time.Instant;

public record BloqueoDTO(
        Long id,
        Long usuarioBloqueadorId,
        String usuarioBloqueadorNombre,
        Long usuarioBloqueadoId,
        String usuarioBloqueadoNombre,
        String tipo,
        String motivo,
        Instant fechaCreacion,
        boolean activo
) {}