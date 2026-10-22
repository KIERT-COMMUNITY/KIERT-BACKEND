package com.kiert.backend.features.bloqueo.dto;

public record CrearBloqueoDTO(
        Long usuarioBloqueadoId,
        String motivo
) {}