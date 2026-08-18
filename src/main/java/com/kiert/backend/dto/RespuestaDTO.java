package com.kiert.backend.dto;

import java.time.Instant;

public record RespuestaDTO(
        Long id,
        AutorResumenDTO autor,
        String contenido,
        Instant fechaCreacion,
        String urlImagen,
        ReaccionesDTO reacciones
) {}