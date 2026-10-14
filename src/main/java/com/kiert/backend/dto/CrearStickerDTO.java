// src/main/java/com/kiert/backend/dto/CrearStickerDTO.java
package com.kiert.backend.dto;

public record CrearStickerDTO(
        String nombre,
        Boolean esPublico
) {}