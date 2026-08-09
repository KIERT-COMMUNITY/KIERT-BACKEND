package com.kiert.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarFotoPerfilDTO(@NotBlank String urlFoto) {}
