package com.kiert.backend.dto;

public record AuthResponseDTO(
        String token,
        UsuarioDTO usuario
) {}