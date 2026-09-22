// src/main/java/com/kiert/backend/dto/InfoInvitacionDTO.java
package com.kiert.backend.dto;

import java.time.Instant;

public record InfoInvitacionDTO(
        boolean valida,
        String mensaje,
        Long grupoId,
        String grupoNombre,
        String grupoDescripcion,
        String grupoFoto,
        String creadorNombre,
        Integer totalMiembros,
        Instant expiraEn
) {}