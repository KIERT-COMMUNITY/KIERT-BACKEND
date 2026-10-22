package com.kiert.backend.features.reacciones.dto;

public record ReaccionesDTO(
        long likes,
        long loves,
        long hahas,
        long wows,
        long sads,
        long angrys
) {}