package com.kiert.backend.features.comentarios.dto;

import com.kiert.backend.features.autor.dto.AutorResumenDTO;
import com.kiert.backend.features.reacciones.dto.ReaccionesDTO;

import java.time.Instant;

public record ComentarioDTO(
        Long id,
        AutorResumenDTO autor,
        String contenido,
        Instant fechaCreacion,
        String urlImagen,
        ReaccionesDTO reacciones
) {}