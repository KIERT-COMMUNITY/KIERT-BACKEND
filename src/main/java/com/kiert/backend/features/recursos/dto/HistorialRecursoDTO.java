// src/main/java/com/kiert/backend/features/recursos/dto/HistorialRecursoDTO.java
package com.kiert.backend.features.recursos.dto;

import java.time.LocalDateTime;

public record HistorialRecursoDTO(
        Long id,
        Long recursoId,
        String accion,
        String datosAnteriores,
        String datosNuevos,
        String ipUsuario,
        String usuarioNombre,
        LocalDateTime fechaAccion
) {}