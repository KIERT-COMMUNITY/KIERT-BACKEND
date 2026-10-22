package com.kiert.backend.features.admin.dtos;

public record AdminDTO(
        Long id,
        String email,
        String nombre,
        String rol
) {
}
