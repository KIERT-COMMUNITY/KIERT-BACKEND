package com.kiert.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordConCodigoDTO(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}") String codigo,
        @NotBlank
        @Size(min = 8)
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contrasena debe tener al menos una mayuscula y un numero")
        String nuevaPassword
) {}