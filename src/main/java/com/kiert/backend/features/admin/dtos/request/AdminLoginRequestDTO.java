package com.kiert.backend.features.admin.dtos.request;

public record AdminLoginRequestDTO(
        String email,
        String password
) {
}
