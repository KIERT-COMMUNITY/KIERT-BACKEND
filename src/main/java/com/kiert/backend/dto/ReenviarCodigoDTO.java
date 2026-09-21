package com.kiert.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReenviarCodigoDTO(@NotBlank @Email String email) {}