package com.kiert.backend.dto;

import jakarta.validation.constraints.NotNull;

public record EnviarSolicitudDTO(
        @NotNull Long usuarioId
) {}