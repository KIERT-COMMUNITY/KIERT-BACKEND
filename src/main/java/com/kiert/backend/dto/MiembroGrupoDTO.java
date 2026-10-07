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
        Boolean online,
        Instant ultimaConexion
) {

    public MiembroGrupoDTO(
            Long id,
            Long usuarioId,
            String nombreUsuario,
            String email,
            String fotoPerfilUrl,
            String rol,
            String estado,
            Instant fechaUnion,
            String invitadoPor
    ) {
        this(
                id,
                usuarioId,
                nombreUsuario,
                email,
                fotoPerfilUrl,
                rol,
                estado,
                fechaUnion,
                invitadoPor,
                false,
                null
        );
    }
}
