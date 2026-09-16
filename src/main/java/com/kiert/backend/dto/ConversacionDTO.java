// src/main/java/com/kiert/backend/dto/ConversacionDTO.java
package com.kiert.backend.dto;

public record ConversacionDTO(
        Long usuarioId,
        String nombreUsuario,
        String fotoPerfilUrl,
        String marcoId,              // ✅ NUEVO
        String ultimoMensaje,
        String ultimaConexion,
        long noLeidos,
        Boolean online
) {}