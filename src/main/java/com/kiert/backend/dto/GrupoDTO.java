package com.kiert.backend.dto;

import java.time.Instant;
import java.util.List;

public record GrupoDTO(
        Long id,
        String nombre,
        String descripcion,
        String fotoUrl,
        Long creadorId,
        String creadorNombre,
        String tipo,
        Instant fechaCreacion,
        Integer totalMiembros,
        List<MiembroGrupoDTO> miembros,
        String rolDelUsuario // Para saber el rol del usuario actual
) {}