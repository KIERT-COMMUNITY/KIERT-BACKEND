// src/main/java/com/kiert/backend/features/grupo/dto/CrearGrupoDTO.java
package com.kiert.backend.features.grupo.dto;

import java.util.List;

public record CrearGrupoDTO(
        String nombre,
        String descripcion,
        String tipo, // PRIVADO, PUBLICO
        List<Long> usuariosInvitados
) {}