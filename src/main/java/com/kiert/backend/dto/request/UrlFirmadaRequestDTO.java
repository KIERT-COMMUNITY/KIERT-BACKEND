package com.kiert.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UrlFirmadaRequestDTO(
        @NotBlank String nombreArchivo,
        @NotBlank String tipoContenido
) {}