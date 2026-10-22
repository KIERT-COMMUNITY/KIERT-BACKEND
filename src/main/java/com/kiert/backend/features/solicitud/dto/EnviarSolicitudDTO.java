package com.kiert.backend.features.solicitud.dto;

import jakarta.validation.constraints.NotNull;

public record EnviarSolicitudDTO(
        @NotNull(message = "El ID del usuario es obligatorio")
        Long usuarioId
) {}