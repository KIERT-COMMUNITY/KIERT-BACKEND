package com.kiert.backend.features.passwordYcodigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RestablecerContrasenaDTO(
        @NotBlank String token,
        @NotBlank
        @Size(min = 8)
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contrasena debe tener al menos una mayuscula y un numero")
        String nuevaContrasena
) {}