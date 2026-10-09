// src/main/java/com/kiert/backend/controller/AuthController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.dto.request.LoginRequestDTO;
import com.kiert.backend.dto.request.RegisterRequestDTO;
import com.kiert.backend.dto.response.AuthResponseDTO;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.repository.UsuarioRepository;
import com.kiert.backend.security.JwtService;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.PresenciaService;
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
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;     // ✅ NUEVO
    private final PresenciaService presenciaService;       // ✅ NUEVO

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
    // LOGIN — ✅ marca en línea al usuario
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO datos) {
        log.info("Login para usuario: {}", datos.email());
        try {
            AuthResponseDTO response = authService.login(datos);

            // ✅ Marcar en línea al usuario recién logueado
            try {
                usuarioRepository.findByEmail(datos.email()).ifPresent(u -> {
                    presenciaService.marcarEnLinea(u.getId());
                    log.info("🟢 Usuario {} EN LÍNEA tras login", u.getId());
                });
            } catch (Exception e) {
                log.warn("No se pudo marcar en línea tras login: {}", e.getMessage());
            }

            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // LOGOUT — ✅ marca desconectado
    // ============================================================
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = extraerTokenDelHeader(request);
        Long usuarioId = extraerUsuarioIdDelToken(token);

        log.info("Logout para usuario: {}", usuarioId);

        if (usuarioId == null) {
            return ResponseEntity.ok().build();
        }

        // ✅ Marcar desconectado ANTES de invalidar el token
        try {
            presenciaService.marcarDesconectado(usuarioId);
            log.info("🔴 Usuario {} DESCONECTADO tras logout", usuarioId);
        } catch (Exception e) {
            log.warn("No se pudo marcar desconectado: {}", e.getMessage());
        }

        authService.logout(usuarioId, token);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // LOGOUT BEACON (para navigator.sendBeacon)
    // ============================================================
    @PostMapping("/logout-beacon")
    public ResponseEntity<Void> logoutBeacon(HttpServletRequest request) {
        String token = extraerTokenDelHeader(request);
        Long usuarioId = extraerUsuarioIdDelToken(token);

        log.info("Logout via beacon para usuario: {}", usuarioId);

        if (usuarioId == null) {
            return ResponseEntity.ok().build();
        }

        // ✅ Marcar desconectado
        try {
            presenciaService.marcarDesconectado(usuarioId);
        } catch (Exception e) {
            log.warn("No se pudo marcar desconectado via beacon: {}", e.getMessage());
        }

        authService.logout(usuarioId, token);
        return ResponseEntity.ok().build();
    }

    // ============================================================
    // RECUPERACION
    // ============================================================
    @PostMapping("/recuperar")
    public ResponseEntity<?> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionDTO datos) {
        log.info("Solicitud de recuperacion para: {}", datos.email());
        try {
            authService.solicitarRecuperacion(datos.email());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Codigo enviado. Revisa tu correo y la carpeta de Spam."));
        } catch (BadRequestException e) {
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Si el correo esta registrado, recibiras un codigo en breve."));
        }
    }

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