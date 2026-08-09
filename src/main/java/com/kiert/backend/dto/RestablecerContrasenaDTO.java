package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RestablecerContrasenaDTO(
        @NotBlank String token,
        @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contraseña debe tener al menos una mayúscula y un número")
        String nuevaContrasena
) {}
