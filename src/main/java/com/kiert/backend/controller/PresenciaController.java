// src/main/java/com/kiert/backend/controller/PresenciaController.java
package com.kiert.backend.controller;

import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.PresenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/presencia")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PresenciaController {

    private final PresenciaService presenciaService;
    private final UsuarioActual usuarioActual;

    /**
     * Heartbeat — el frontend lo llama cada minuto.
     * ✅ NO lanza 401 si no hay usuario (es endpoint público).
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat() {
        Long uid = usuarioActual.id();
        if (uid == null) {
            // ✅ Devolver 200 con ok:false en lugar de 401
            // Esto evita que el interceptor del frontend te desloguee
            log.debug("⚠️ Heartbeat sin usuario autenticado (público)");
            return ResponseEntity.ok(Map.of("ok", false, "online", false));
        }
        presenciaService.marcarEnLinea(uid);
        log.debug("💓 Heartbeat OK para usuario {}", uid);
        return ResponseEntity.ok(Map.of("ok", true, "online", true));
    }

    /**
     * Offline explícito.
     * ✅ Igual que heartbeat: no lanza 401 si no hay usuario.
     */
    @PostMapping("/offline")
    public ResponseEntity<?> offline() {
        Long uid = usuarioActual.id();
        if (uid == null) {
            return ResponseEntity.ok(Map.of("ok", false, "online", false));
        }
        presenciaService.marcarDesconectado(uid);
        log.debug("🔴 Offline para usuario {}", uid);
        return ResponseEntity.ok(Map.of("ok", true, "online", false));
    }
}