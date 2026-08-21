package com.kiert.backend.dto;

public record ConversacionDTO(
        Long usuarioId,
        String nombreUsuario,
        String fotoPerfilUrl,
        String ultimoMensaje,
        String ultimaConexion,
        long noLeidos
) {}