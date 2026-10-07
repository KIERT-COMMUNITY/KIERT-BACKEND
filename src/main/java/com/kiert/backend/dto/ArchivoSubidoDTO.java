package com.kiert.backend.dto;

public record ArchivoSubidoDTO(
        String nombreOriginal,
        String secureUrl,
        String publicId,
        String tipoMime,
        String tipoArchivo,
        String formato,
        String resourceType,
        Long tamanoBytes,
        Double duracionSegundos,
        Integer ancho,
        Integer alto
) {
}
