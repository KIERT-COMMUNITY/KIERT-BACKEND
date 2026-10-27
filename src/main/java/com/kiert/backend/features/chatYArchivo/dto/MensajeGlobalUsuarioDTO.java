package com.kiert.backend.features.chatYArchivo.dto;

public record MensajeGlobalUsuarioDTO(
        Long id,
        Long emisorId,
        String emisorNombre,
        String emisorFoto,
        String emisorMarcoId,
        String contenido,
        String tipoMensaje,
        String fechaEnvio,
        boolean propio
) {}