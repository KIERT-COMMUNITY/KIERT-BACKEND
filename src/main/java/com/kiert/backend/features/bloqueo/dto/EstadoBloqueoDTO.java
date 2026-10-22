package com.kiert.backend.features.bloqueo.dto;

public record EstadoBloqueoDTO(
        boolean bloqueado,
        Long bloqueoId,
        String motivo,
        String fechaBloqueo
) {}