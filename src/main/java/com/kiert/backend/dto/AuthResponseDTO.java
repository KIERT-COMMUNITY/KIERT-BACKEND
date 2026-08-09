package com.kiert.backend.dto;

// Espejo de AuthResponse en user.model.ts (lo que devuelve login/registro)
public record AuthResponseDTO(
        String token,
        UsuarioDTO usuario
) {}
