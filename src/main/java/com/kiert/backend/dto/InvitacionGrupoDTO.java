package com.kiert.backend.dto;

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