// src/main/java/com/kiert/backend/dto/ConversacionDTO.java
package com.kiert.backend.dto;

public record ConversacionDTO(
        Long usuarioId,
        String nombreUsuario,
        String fotoPerfilUrl,
        String marcoId,
        String ultimoMensaje,
        String fechaUltimoMensaje,      // ISO sin microsegundos
        long noLeidos,
        Boolean online,
        String ultimaConexion           // ✅ ISO con milisegundos (JS lo parsea)
) {}