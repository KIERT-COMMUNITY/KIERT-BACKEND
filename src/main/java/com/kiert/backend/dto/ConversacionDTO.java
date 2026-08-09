package com.kiert.backend.dto;

// Espejo de Conversacion en chat.model.ts
public record ConversacionDTO(
        Long usuarioId,
        String nombreUsuario,
        String fotoPerfilUrl,
        String ultimoMensaje,
        String ultimaConexion,
        long noLeidos
) {}
