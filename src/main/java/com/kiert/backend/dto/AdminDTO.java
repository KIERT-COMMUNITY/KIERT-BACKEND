package com.kiert.backend.dto;

public record AdminDTO(
        Long id,
        String email,
        String nombre,
        String rol
) {
}
