package com.kiert.backend.features.reacciones.dto;

import jakarta.validation.constraints.NotBlank;

public record ReaccionDTO(
        @NotBlank String tipo
) {}