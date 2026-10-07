// src/main/java/com/kiert/backend/dto/ConversacionDTO.java
package com.kiert.backend.dto;

public record ConversacionDTO(
        Long usuarioId,
        String nombreUsuario,
        String fotoPerfilUrl,
        String marcoId,
        String ultimoMensaje,
        String ultimaConexion,      // ← el que tienes
        long noLeidos,
        Boolean online,
        String ultimaConexionReal   // ← 9no campo (¡ya lo tienes añadido!)
) {}