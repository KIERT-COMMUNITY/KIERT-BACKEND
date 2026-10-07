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
        String rolDelUsuario,
        Integer miembrosEnLinea
) {

    public GrupoDTO(
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
            String rolDelUsuario
    ) {
        this(
                id,
                nombre,
                descripcion,
                fotoUrl,
                creadorId,
                creadorNombre,
                tipo,
                fechaCreacion,
                totalMiembros,
                miembros,
                rolDelUsuario,
                0
        );
    }
}
