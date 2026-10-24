// src/main/java/com/kiert/backend/features/grupo/dto/MiembroGrupoDTO.java
package com.kiert.backend.features.grupo.dto;

import java.time.Instant;

public record MiembroGrupoDTO(
        Long id,
        Long usuarioId,
        String nombreUsuario,
        String email,
        String fotoPerfilUrl,
        String marcoId,
        String rol,
        String estado,
        Instant fechaUnion,
        String invitadoPor,
        Boolean enLinea,
        Instant ultimaConexion
) {}