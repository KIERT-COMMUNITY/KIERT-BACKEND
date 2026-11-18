// src/main/java/com/kiert/backend/features/reacciones/dto/ReaccionDTO.java
package com.kiert.backend.features.reacciones.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para reaccionar a un post o comentario.
 * El campo `tipo` es obligatorio (like, love, haha, wow, sad, angry).
 */
public record ReaccionDTO(
        @NotBlank String tipo
) {}