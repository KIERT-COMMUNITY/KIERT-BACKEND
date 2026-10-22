package com.kiert.backend.features.reporte.dto;

public record CrearReporteDTO(
        String tipoReporte,      // POST, COMENTARIO, RESPUESTA, USUARIO
        String motivo,           // SPAM, ACOSO, etc.
        String descripcion,      // Explicación del usuario
        Long postId,
        Long comentarioId,
        Long respuestaId,
        Long usuarioReportadoId
) {}