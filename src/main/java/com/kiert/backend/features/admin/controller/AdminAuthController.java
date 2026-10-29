// src/main/java/com/kiert/backend/features/admin/controller/AdminAuthController.java
package com.kiert.backend.features.admin.controller;

import com.kiert.backend.features.admin.dtos.request.AdminLoginRequestDTO;
import com.kiert.backend.features.admin.dtos.response.AdminLoginResponseDTO;
import com.kiert.backend.features.admin.services.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginRequestDTO req) {
        try {
            AdminLoginResponseDTO response = adminService.login(req);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[ADMIN] Error inesperado en login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar el login"));
        }
    }
}