// src/main/java/com/kiert/backend/features/grupo/dto/MensajeGrupoDTO.java
package com.kiert.backend.features.grupo.dto;

import com.kiert.backend.features.mensajes.dto.MensajeArchivoDTO;

import java.time.Instant;
import java.util.List;

public record MensajeGrupoDTO(
        Long id,
        Long grupoId,
        Long emisorId,
        String emisorNombre,
        String emisorFoto,
        String marcoEmisor,
        String contenido,
        String tipoMensaje,
        String urlArchivo,
        String nombreArchivo,
        Instant fechaEnvio,
        boolean propio,
        List<MensajeArchivoDTO> archivos,
        String stickerUrl,
        String stickerNombre
) {

    /** Constructor corto (sin archivos ni sticker) */
    public MensajeGrupoDTO(
            Long id, Long grupoId, Long emisorId, String emisorNombre, String emisorFoto,
            String contenido, String tipoMensaje, String urlArchivo, String nombreArchivo,
            Instant fechaEnvio, boolean propio
    ) {
        this(
                id, grupoId, emisorId, emisorNombre, emisorFoto,
                "none",
                contenido, tipoMensaje, urlArchivo, nombreArchivo,
                fechaEnvio, propio, null, null, null
        );
    }

    /** Constructor medio (con archivos, sin sticker) */
    public MensajeGrupoDTO(
            Long id, Long grupoId, Long emisorId, String emisorNombre, String emisorFoto,
            String contenido, String tipoMensaje, String urlArchivo, String nombreArchivo,
            Instant fechaEnvio, boolean propio,
            List<MensajeArchivoDTO> archivos
    ) {
        this(
                id, grupoId, emisorId, emisorNombre, emisorFoto,
                "none",
                contenido, tipoMensaje, urlArchivo, nombreArchivo,
                fechaEnvio, propio, archivos, null, null
        );
    }
}