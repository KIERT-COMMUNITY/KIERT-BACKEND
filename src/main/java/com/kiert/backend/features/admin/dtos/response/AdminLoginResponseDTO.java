package com.kiert.backend.features.admin.dtos.response;

import com.kiert.backend.features.admin.dtos.AdminDTO;

public record AdminLoginResponseDTO (
    String token,
    AdminDTO admin,
    long expiresIn
){}
