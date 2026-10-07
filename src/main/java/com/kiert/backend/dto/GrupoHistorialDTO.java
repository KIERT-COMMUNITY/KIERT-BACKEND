// src/main/java/com/kiert/backend/dto/GrupoHistorialDTO.java
package com.kiert.backend.dto;

import java.time.Instant;

public record GrupoHistorialDTO(
        Long id,
        Long grupoId,
        Long usuarioId,
        String usuarioNombre,
        String usuarioFoto,
        String accion,
        String detalle,
        String valorAnterior,
        String valorNuevo,
        Instant fecha
) {}