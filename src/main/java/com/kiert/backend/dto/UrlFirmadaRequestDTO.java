package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UrlFirmadaRequestDTO(
        @NotBlank String nombreArchivo,
        @NotBlank String tipoContenido
) {}