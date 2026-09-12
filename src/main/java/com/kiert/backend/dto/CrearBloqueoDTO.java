package com.kiert.backend.dto;

public record CrearBloqueoDTO(
        Long usuarioBloqueadoId,
        String motivo
) {}