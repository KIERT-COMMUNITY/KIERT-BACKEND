package com.kiert.backend.dto.response;

import com.kiert.backend.dto.AdminDTO;

public record AdminLoginResponseDTO (
    String token,
    AdminDTO admin,
    long expiresIn
){}
