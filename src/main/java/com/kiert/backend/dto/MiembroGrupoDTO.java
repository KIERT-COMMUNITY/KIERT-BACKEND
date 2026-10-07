// src/main/java/com/kiert/backend/dto/MiembroGrupoDTO.java
package com.kiert.backend.dto;

import java.time.Instant;

public record MiembroGrupoDTO(
        Long id,
        Long usuarioId,
        String nombreUsuario,
        String email,
        String fotoPerfilUrl,
        String rol,
        String estado,
        Instant fechaUnion,
        String invitadoPor,
        Boolean enLinea,
        Instant ultimaConexion
) {}