package com.kiert.backend.dto;

import java.time.Instant;

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
        boolean propio
) {}