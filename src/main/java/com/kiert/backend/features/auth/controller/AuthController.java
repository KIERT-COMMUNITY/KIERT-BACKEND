// src/main/java/com/kiert/backend/features/auth/controller/AuthController.java
package com.kiert.backend.features.auth.controller;

import com.kiert.backend.features.login.request.LoginRequestDTO;
import com.kiert.backend.features.login.request.RegisterRequestDTO;
import com.kiert.backend.features.auth.dto.response.AuthResponseDTO;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.features.passwordYcodigo.dto.ReenviarCodigoDTO;
import com.kiert.backend.features.passwordYcodigo.dto.ResetPasswordConCodigoDTO;
import com.kiert.backend.features.passwordYcodigo.dto.SolicitarRecuperacionDTO;
import com.kiert.backend.features.passwordYcodigo.dto.VerificarCodigoDTO;
import com.kiert.backend.features.auth.service.AuthService;
import com.kiert.backend.security.JwtService;
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
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    private static final String PREFIJO_BEARER = "Bearer ";

    // ============================================================
    // REGISTRO
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
    // VERIFICAR CUENTA
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
    // REENVIAR CODIGO
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
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = extraerTokenDelHeader(request);
        Long usuarioId = extraerUsuarioIdDelToken(token);

        log.info("Logout para usuario: {}", usuarioId);
        authService.logout(usuarioId, token);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // LOGOUT BEACON
    // ============================================================
    @PostMapping("/logout-beacon")
    public ResponseEntity<Void> logoutBeacon(HttpServletRequest request) {
        String token = extraerTokenDelHeader(request);
        Long usuarioId = extraerUsuarioIdDelToken(token);

        log.info("Logout via beacon para usuario: {}", usuarioId);
        authService.logout(usuarioId, token);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // SOLICITAR RECUPERACION
    // ============================================================
    @PostMapping("/recuperar")
    public ResponseEntity<?> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionDTO datos) {
        log.info("Solicitud de recuperacion para: {}", datos.email());
        try {
            authService.solicitarRecuperacion(datos.email());
        } catch (BadRequestException e) {
            // No revelar si el email existe. Respuesta generica.
        }
        return ResponseEntity.ok(Map.of(
                "mensaje", "Si el correo esta registrado, recibiras un codigo en breve."));
    }

    // ============================================================
    // NUEVO: VERIFICAR CODIGO DE RECUPERACION
    // ============================================================
    @PostMapping("/verificar-codigo-recuperacion")
    public ResponseEntity<?> verificarCodigoRecuperacion(@Valid @RequestBody VerificarCodigoDTO req) {
        log.info("Verificando codigo de recuperacion para: {}", req.email());
        try {
            String tokenTemporal = authService.verificarCodigoRecuperacion(req.email(), req.codigo());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Codigo verificado correctamente",
                    "resetToken", tokenTemporal
            ));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // RESET PASSWORD CON CODIGO (compatibilidad flujo antiguo)
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
    // NUEVO: RESET PASSWORD CON TOKEN TEMPORAL (flujo del frontend nuevo)
    // ============================================================
    @PostMapping("/reset-password-token")
    public ResponseEntity<?> resetPasswordConToken(@RequestBody Map<String, String> body) {
        String resetToken = body.get("token");
        String nuevaPassword = body.get("nuevaPassword");

        if (resetToken == null || nuevaPassword == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Token y nueva contrasena son obligatorios"));
        }

        log.info("Reset password con token temporal");
        try {
            authService.restablecerPasswordConToken(resetToken, nuevaPassword);
            return ResponseEntity.ok(Map.of("mensaje", "Contrasena actualizada correctamente"));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private String extraerTokenDelHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            return null;
        }
        return header.substring(PREFIJO_BEARER.length());
    }

    private Long extraerUsuarioIdDelToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return jwtService.extraerUsuarioId(token);
        } catch (Exception e) {
            log.warn("No se pudo extraer usuarioId del token en logout: {}", e.getMessage());
            return null;
        }
    }
}