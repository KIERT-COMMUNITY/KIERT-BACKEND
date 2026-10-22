package com.kiert.backend.features.auth.dto.response;

import com.kiert.backend.features.usuario.dto.UsuarioDTO;

public record AuthResponseDTO(
        String token,
        UsuarioDTO usuario
) {}