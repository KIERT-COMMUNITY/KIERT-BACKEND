package com.kiert.backend.features.solicitud.dto;

import java.time.Instant;

public record SolicitudContactoDTO(
        Long id,
        Long usuarioId,
        String nombreUsuario,
        String fotoPerfilUrl,
        String estado,
        Instant fechaSolicitud
) {}