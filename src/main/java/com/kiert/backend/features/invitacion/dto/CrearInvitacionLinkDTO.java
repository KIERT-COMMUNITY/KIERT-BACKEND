// src/main/java/com/kiert/backend/dto/CrearInvitacionLinkDTO.java
package com.kiert.backend.features.invitacion.dto;

public record CrearInvitacionLinkDTO(
        Integer usosMaximos,     // null o 0 = ilimitado
        Integer horasExpiracion  // null = nunca expira
) {}