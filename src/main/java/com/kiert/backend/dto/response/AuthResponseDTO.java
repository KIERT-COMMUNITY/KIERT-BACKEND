package com.kiert.backend.dto.response;

import com.kiert.backend.dto.UsuarioDTO;

public record AuthResponseDTO(
        String token,
        UsuarioDTO usuario
) {}