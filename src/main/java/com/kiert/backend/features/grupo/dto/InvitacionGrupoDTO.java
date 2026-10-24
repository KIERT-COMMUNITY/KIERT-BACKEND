// src/main/java/com/kiert/backend/features/grupo/dto/InvitacionGrupoDTO.java
package com.kiert.backend.features.grupo.dto;

import java.time.Instant;

public record InvitacionGrupoDTO(
        Long id,
        Long grupoId,
        String grupoNombre,
        String grupoFoto,
        Long invitadorId,
        String invitadorNombre,
        Instant fechaInvitacion
) {}