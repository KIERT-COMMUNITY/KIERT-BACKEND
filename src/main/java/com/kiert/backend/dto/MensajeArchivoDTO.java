package com.kiert.backend.dto;

public record MensajeArchivoDTO(
        Long id,
        String nombre,
        String url,
        String tipo,
        Integer pesoKb,
        boolean esSensible,
        String publicId,
        String tipoMime,
        String formato,
        String resourceType,
        Long tamanoBytes,
        Double duracionSegundos,
        Integer ancho,
        Integer alto
) {

    public MensajeArchivoDTO(
            Long id,
            String nombre,
            String url,
            String tipo,
            Integer pesoKb,
            boolean esSensible
    ) {
        this(
                id,
                nombre,
                url,
                tipo,
                pesoKb,
                esSensible,
                null,
                null,
                null,
                null,
                pesoKb == null ? null : pesoKb.longValue() * 1024L,
                null,
                null,
                null
        );
    }
}
