package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;

// Espejo del body que envía upload.service.ts -> pedirUrlFirmada()
public record UrlFirmadaRequestDTO(
        @NotBlank String nombreArchivo,
        @NotBlank String tipoContenido
) {}
