package com.kiert.backend.dto;

public record AdjuntoDTO(
        Long id,
        String tipo,
        String nombre,
        String url,
        Integer pesoKb,
        Integer duracionSegundos,
        Integer ancho,
        Integer alto,
        String formato
) {
    // Constructor para compatibilidad con código existente
    public AdjuntoDTO(Long id, String tipo, String nombre, String url, Integer pesoKb) {
        this(id, tipo, nombre, url, pesoKb, null, null, null, null);
    }
}