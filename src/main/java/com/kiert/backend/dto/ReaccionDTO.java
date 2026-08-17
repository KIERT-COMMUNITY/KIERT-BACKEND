package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ReaccionDTO(
        @NotBlank String tipo
) {}