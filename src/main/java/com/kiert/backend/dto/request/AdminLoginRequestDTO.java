package com.kiert.backend.dto.request;

public record AdminLoginRequestDTO(
        String email,
        String password
) {
}
