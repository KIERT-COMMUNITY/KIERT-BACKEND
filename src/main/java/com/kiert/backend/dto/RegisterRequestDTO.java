package com.kiert.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
        @NotBlank @Size(min = 3, max = 20) String nombreUsuario,
        @NotBlank @Email String email,
        // al menos 1 mayúscula, 1 número, mínimo 8 caracteres (igual que el frontend)
        @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contraseña debe tener al menos una mayúscula y un número")
        String password
) {}
