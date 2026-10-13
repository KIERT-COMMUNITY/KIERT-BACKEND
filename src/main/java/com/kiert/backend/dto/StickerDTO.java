// src/main/java/com/kiert/backend/dto/StickerDTO.java
package com.kiert.backend.dto;

import java.time.Instant;

public record StickerDTO(
        Long id,
        Long usuarioId,
        String nombre,
        String urlImagen,
        String urlMiniatura,
        String tipoArchivo,
        Integer pesoKb,
        Integer ancho,
        Integer alto,
        Boolean esPublico,
        Instant fechaCreacion
) {}