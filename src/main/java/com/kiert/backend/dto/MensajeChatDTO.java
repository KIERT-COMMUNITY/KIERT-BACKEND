// src/main/java/com/kiert/backend/dto/MensajeChatDTO.java
package com.kiert.backend.dto;

import java.time.Instant;
import java.util.List;

public record MensajeChatDTO(
        Long id,
        Long emisorId,
        String contenido,
        Instant fechaEnvio,
        boolean propio,
        List<MensajeArchivoDTO> archivos,
        String tipoMensaje,
        String stickerUrl,
        String stickerNombre
) {
    // Constructor sin sticker ni archivos
    public MensajeChatDTO(Long id, Long emisorId, String contenido,
                          Instant fechaEnvio, boolean propio) {
        this(id, emisorId, contenido, fechaEnvio, propio, null, "TEXTO", null, null);
    }

    // Constructor con archivos pero sin sticker
    public MensajeChatDTO(Long id, Long emisorId, String contenido,
                          Instant fechaEnvio, boolean propio,
                          List<MensajeArchivoDTO> archivos) {
        this(id, emisorId, contenido, fechaEnvio, propio, archivos, "TEXTO", null, null);
    }
}