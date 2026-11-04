// src/main/java/com/kiert/backend/features/login/request/LoginRequestDTO.java
package com.kiert.backend.features.login.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank(message = "El token de seguridad es obligatorio") String turnstileToken
) {}