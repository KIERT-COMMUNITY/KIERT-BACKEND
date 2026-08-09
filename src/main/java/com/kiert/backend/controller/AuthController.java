package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Espejo exacto de las rutas que llama auth.service.ts en el frontend.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<AuthResponseDTO> registro(@Valid @RequestBody RegisterRequestDTO datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(datos));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO datos) {
        return ResponseEntity.ok(authService.login(datos));
    }

    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<MensajeSimpleDTO> recuperarContrasena(@Valid @RequestBody SolicitarRecuperacionDTO datos) {
        return ResponseEntity.ok(authService.solicitarRecuperacion(datos.email()));
    }

    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<MensajeSimpleDTO> restablecerContrasena(@Valid @RequestBody RestablecerContrasenaDTO datos) {
        return ResponseEntity.ok(authService.restablecerContrasena(datos.token(), datos.nuevaContrasena()));
    }
}
