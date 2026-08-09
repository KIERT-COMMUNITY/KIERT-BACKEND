package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record EnviarMensajeDTO(@NotBlank String contenido) {}
