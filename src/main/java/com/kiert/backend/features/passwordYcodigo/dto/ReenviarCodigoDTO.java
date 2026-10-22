package com.kiert.backend.features.passwordYcodigo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReenviarCodigoDTO(@NotBlank @Email String email) {}