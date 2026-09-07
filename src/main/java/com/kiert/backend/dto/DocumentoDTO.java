package com.kiert.backend.dto;

import java.time.Instant;

public record DocumentoDTO(
        Long id,
        String titulo,
        String descripcion,
        String categoria,
        String categoriaPersonalizada,
        String urlArchivo,
        String nombreArchivo,
        String tipoArchivo,
        Long tamanoKb,
        AutorResumenDTO autor,
        Long descargas,
        Long visitas,
        Instant fechaCreacion
) {}