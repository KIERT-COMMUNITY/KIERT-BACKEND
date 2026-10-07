// src/main/java/com/kiert/backend/controller/AuthController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.dto.request.LoginRequestDTO;
import com.kiert.backend.dto.request.RegisterRequestDTO;
import com.kiert.backend.dto.response.AuthResponseDTO;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.security.JwtService;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtService jwtService;   // ← NUEVO: para extraer el usuario del token en logout

    private static final String PREFIJO_BEARER = "Bearer ";

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
    // LOGOUT (con blacklist de JWT)
    // ============================================================
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        // Extraer el token del header Authorization
        String token = extraerTokenDelHeader(request);

        // Extraer el usuarioId del token
        Long usuarioId = extraerUsuarioIdDelToken(token);

        log.info("Logout para usuario: {}", usuarioId);

        // Si no hay token válido, no hay nada que invalidar
        if (usuarioId == null) {
            return ResponseEntity.ok().build();
        }

        authService.logout(usuarioId, token);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // LOGOUT BEACON (para navigator.sendBeacon)
    // ============================================================
    /**
     * ⚠️ CAMBIO IMPORTANTE: ya NO acepta usuarioId como @RequestParam.
     * Antes, cualquiera podía hacer logout de otro usuario.
     *
     * Ahora extrae el usuarioId del token (que sí es seguro).
     */
    @PostMapping("/logout-beacon")
    public ResponseEntity<Void> logoutBeacon(HttpServletRequest request) {
        String token = extraerTokenDelHeader(request);
        Long usuarioId = extraerUsuarioIdDelToken(token);

        log.info("Logout via beacon para usuario: {}", usuarioId);

        if (usuarioId == null) {
            return ResponseEntity.ok().build();
        }

        authService.logout(usuarioId, token);
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

    // ============================================================
    // HELPERS PRIVADOS
    // ============================================================

    /**
     * Extrae el token JWT del header Authorization.
     * Devuelve null si no existe o no tiene el prefijo Bearer.
     */
    private String extraerTokenDelHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            return null;
        }
        return header.substring(PREFIJO_BEARER.length());
    }

    /**
     * Extrae el usuarioId del token JWT.
     * Devuelve null si el token es inválido o ha expirado.
     */
    private Long extraerUsuarioIdDelToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return jwtService.extraerUsuarioId(token);
        } catch (Exception e) {
            log.warn("No se pudo extraer usuarioId del token en logout: {}", e.getMessage());
            return null;
        }
    }
}