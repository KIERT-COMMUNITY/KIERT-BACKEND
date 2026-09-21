package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.dto.request.LoginRequestDTO;
import com.kiert.backend.dto.request.RegisterRequestDTO;
import com.kiert.backend.dto.response.AuthResponseDTO;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS
})
public class AuthController {

    private final AuthService authService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // REGISTRO -> envia codigo al email
    // ============================================================
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegisterRequestDTO datos) {
        log.info("Registrando usuario: {}", datos.email());
        try {
            authService.registrar(datos);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Cuenta creada. Revisa tu correo (incluida la carpeta de Spam) para el codigo de verificacion.",
                    "email", datos.email()
            ));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // VERIFICAR CUENTA CON CODIGO
    // ============================================================
    @PostMapping("/verificar-cuenta")
    public ResponseEntity<?> verificarCuenta(@Valid @RequestBody VerificarCodigoDTO req) {
        log.info("Verificando cuenta: {}", req.email());
        try {
            authService.verificarCuenta(req.email(), req.codigo());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Cuenta verificada correctamente. Bienvenido a Kiert!"));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // REENVIAR CODIGO DE VERIFICACION
    // ============================================================
    @PostMapping("/reenviar-codigo")
    public ResponseEntity<?> reenviarCodigo(@Valid @RequestBody ReenviarCodigoDTO req) {
        log.info("Reenviando codigo a: {}", req.email());
        try {
            authService.reenviarCodigoVerificacion(req.email());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Codigo reenviado. Revisa tu correo y la carpeta de Spam."));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // LOGIN
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO datos) {
        log.info("Login para usuario: {}", datos.email());
        try {
            AuthResponseDTO response = authService.login(datos);
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // LOGOUT
    // ============================================================
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Long usuarioId = usuarioActual.id();
        log.info("Logout para usuario: {}", usuarioId);
        authService.logout(usuarioId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-beacon")
    public ResponseEntity<Void> logoutBeacon(@RequestParam(required = false) Long usuarioId) {
        log.info("Logout via beacon para usuario: {}", usuarioId);
        authService.logout(usuarioId);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // SOLICITAR RECUPERACION -> envia codigo
    // ============================================================
    @PostMapping("/recuperar")
    public ResponseEntity<?> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionDTO datos) {
        log.info("Solicitud de recuperacion para: {}", datos.email());
        try {
            authService.solicitarRecuperacion(datos.email());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Codigo enviado. Revisa tu correo y la carpeta de Spam."));
        } catch (BadRequestException e) {
            // Por seguridad, no revelamos si el email existe o no
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Si el correo esta registrado, recibiras un codigo en breve."));
        }
    }

    // ============================================================
    // RESET PASSWORD CON CODIGO
    // ============================================================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordConCodigoDTO req) {
        log.info("Reset password para: {}", req.email());
        try {
            authService.restablecerPasswordConCodigo(req.email(), req.codigo(), req.nuevaPassword());
            return ResponseEntity.ok(Map.of("mensaje", "Contrasena actualizada correctamente"));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}