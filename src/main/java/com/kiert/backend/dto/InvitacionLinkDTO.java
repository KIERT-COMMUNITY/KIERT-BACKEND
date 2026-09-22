// src/main/java/com/kiert/backend/dto/InvitacionLinkDTO.java
package com.kiert.backend.dto;

import java.time.Instant;

public record InvitacionLinkDTO(
        Long id,
        Long grupoId,
        String grupoNombre,
        String token,
        String urlInvitacion,
        Long creadorId,
        String creadorNombre,
        Integer usosMaximos,
        Integer usosActuales,
        Instant expiraEn,
        boolean activo,
        Instant fechaCreacion,
        Instant fechaUltimoUso
) {}