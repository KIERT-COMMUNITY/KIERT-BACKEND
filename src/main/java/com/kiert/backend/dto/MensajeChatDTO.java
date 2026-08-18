package com.kiert.backend.dto;

import java.time.Instant;
import java.util.List;

public record MensajeChatDTO(
        Long id,
        Long emisorId,
        String contenido,
        Instant fechaEnvio,
        boolean propio,
        List<MensajeArchivoDTO> archivos
) {
    // Constructor para mensajes sin archivos
    public MensajeChatDTO(Long id, Long emisorId, String contenido, Instant fechaEnvio, boolean propio) {
        this(id, emisorId, contenido, fechaEnvio, propio, null);
    }
}