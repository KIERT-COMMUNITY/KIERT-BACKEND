package com.kiert.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotBlank @Size(min = 3, max = 20) String nombreUsuario,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contraseña debe tener al menos una mayúscula y un número")
        String password
) {}