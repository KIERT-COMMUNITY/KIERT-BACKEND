package com.kiert.backend.dto;

public record EstadoBloqueoDTO(
        boolean bloqueado,
        Long bloqueoId,
        String motivo,
        String fechaBloqueo
) {}