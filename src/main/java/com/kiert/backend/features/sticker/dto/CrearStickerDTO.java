// src/main/java/com/kiert/backend/dto/CrearStickerDTO.java
package com.kiert.backend.features.sticker.dto;

public record CrearStickerDTO(
        String nombre,
        Boolean esPublico
) {}