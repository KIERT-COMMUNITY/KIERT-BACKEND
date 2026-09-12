package com.kiert.backend.dto;

import java.util.List;

public record CrearGrupoDTO(
        String nombre,
        String descripcion,
        String tipo, // PRIVADO, PUBLICO
        List<Long> usuariosInvitados
) {}