package com.kiert.backend.dto;

import java.time.Instant;
import java.util.List;

public record MensajeGrupoDTO(
        Long id,
        Long grupoId,
        Long emisorId,
        String emisorNombre,
        String emisorFoto,
        String contenido,
        String tipoMensaje,
        String urlArchivo,
        String nombreArchivo,
        Instant fechaEnvio,
        boolean propio,
        List<MensajeArchivoDTO> archivos
) {

    public MensajeGrupoDTO(
            Long id,
            Long grupoId,
            Long emisorId,
            String emisorNombre,
            String emisorFoto,
            String contenido,
            String tipoMensaje,
            String urlArchivo,
            String nombreArchivo,
            Instant fechaEnvio,
            boolean propio
    ) {
        this(
                id,
                grupoId,
                emisorId,
                emisorNombre,
                emisorFoto,
                contenido,
                tipoMensaje,
                urlArchivo,
                nombreArchivo,
                fechaEnvio,
                propio,
                null
        );
    }
}
