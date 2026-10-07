package com.kiert.backend.dto;

public record ConversacionDTO(
        Long usuarioId,
        String nombreUsuario,
        String fotoPerfilUrl,
        String marcoId,
        String ultimoMensaje,
        String ultimoMensajeFecha,
        String ultimaConexion,
        long noLeidos,
        Boolean online
) {

    public ConversacionDTO(
            Long usuarioId,
            String nombreUsuario,
            String fotoPerfilUrl,
            String marcoId,
            String ultimoMensaje,
            String ultimoMensajeFecha,
            long noLeidos,
            Boolean online
    ) {
        this(
                usuarioId,
                nombreUsuario,
                fotoPerfilUrl,
                marcoId,
                ultimoMensaje,
                ultimoMensajeFecha,
                null,
                noLeidos,
                online
        );
    }
}
