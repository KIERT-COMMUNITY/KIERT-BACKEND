package com.kiert.backend.features.bienvenida.dto;

public record HistorialBienvenidaDTO(
        Long id,
        String tituloAnterior,
        String contenidoAnterior,
        String tituloNuevo,
        String contenidoNuevo,
        Long cambiadoPorId,
        String cambiadoPorNombre,
        String fechaCambio,
        String ipCambio
) {}