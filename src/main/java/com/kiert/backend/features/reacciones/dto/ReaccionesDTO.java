// src/main/java/com/kiert/backend/features/reacciones/dto/ReaccionesDTO.java
package com.kiert.backend.features.reacciones.dto;

/**
 * DTO de respuesta para las reacciones de un post o comentario.
 * Las claves coinciden EXACTAMENTE con lo que espera el frontend
 * (ReaccionResponse en reaccion.service.ts):
 *   likes, loves, hahas, wows, sads, angrys
 */
public record ReaccionesDTO(
        long likes,
        long loves,
        long hahas,
        long wows,
        long sads,
        long angrys
) {}