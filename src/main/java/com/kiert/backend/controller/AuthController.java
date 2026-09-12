package com.kiert.backend.controller;

import com.kiert.backend.dto.response.AuthResponseDTO;
import com.kiert.backend.dto.request.LoginRequestDTO;
import com.kiert.backend.dto.request.RegisterRequestDTO;
import com.kiert.backend.dto.SolicitarRecuperacionDTO;
import com.kiert.backend.dto.RestablecerContrasenaDTO;
import com.kiert.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})

public class AuthController {


    private final AuthService authService;



    @PostMapping("/registro")
    public ResponseEntity<AuthResponseDTO> registrar(@Valid @RequestBody RegisterRequestDTO datos) {
        log.info("📝 Registrando usuario: {}", datos.email());
        return ResponseEntity.ok(authService.registrar(datos));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO datos) {
        log.info("🔑 Login para usuario: {}", datos.email());
        return ResponseEntity.ok(authService.login(datos));
    }

    @PostMapping("/recuperar")
    public ResponseEntity<Void> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionDTO datos) {
        log.info("📧 Solicitud de recuperación para: {}", datos.email());
        authService.solicitarRecuperacion(datos.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restablecer")
    public ResponseEntity<Void> restablecerContrasena(@Valid @RequestBody RestablecerContrasenaDTO datos) {
        log.info("🔑 Restableciendo contraseña");
        authService.restablecerContrasena(datos.token(), datos.nuevaContrasena());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validar-token")
    public ResponseEntity<Boolean> validarToken(@RequestParam String token) {
        log.info("🔍 Validando token");
        return ResponseEntity.ok(authService.validarToken(token));
    }
}