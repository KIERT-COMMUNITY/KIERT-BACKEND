package com.kiert.backend.dto;

import java.time.Instant;

// DTO de respuesta
public record ReporteDTO(
        Long id,
        String tipoReporte,
        String motivo,
        String descripcion,
        String estado,
        Instant fechaCreacion,
        // Info del reportante
        Long reportanteId,
        String reportanteNombre,
        String reportanteFoto,
        // Info del reportado
        Long reportadoId,
        String reportadoNombre,
        String reportadoFoto,
        // Contenido reportado
        Long postId,
        String postTitulo,
        Long comentarioId,
        String comentarioContenido,
        Long respuestaId,
        String respuestaContenido,
        // Moderación
        Long revisadoPorId,
        String revisadoPorNombre,
        Instant fechaRevision,
        String notaModerador,
        String accionTomada
) {}