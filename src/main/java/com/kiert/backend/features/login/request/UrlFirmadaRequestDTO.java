package com.kiert.backend.features.login.request;

import jakarta.validation.constraints.NotBlank;

public record UrlFirmadaRequestDTO(
        @NotBlank String nombreArchivo,
        @NotBlank String tipoContenido
) {}