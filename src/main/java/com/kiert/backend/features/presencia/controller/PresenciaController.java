// src/main/java/com/kiert/backend/features/presencia/controller/PresenciaController.java
package com.kiert.backend.features.presencia.controller;

import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.presencia.service.PresenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/presencia")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PresenciaController {

    private static final int MAX_IDS_CONSULTA_MULTIPLE = 100;

    private final PresenciaService presenciaService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // HEARTBEAT
    // ============================================================
    @PostMapping("/heartbeat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> heartbeat() {
        Long uid = usuarioActual.id();
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "online", false));
        }
        presenciaService.marcarEnLinea(uid);
        log.debug("Heartbeat OK para usuario {}", uid);
        return ResponseEntity.ok(Map.of("ok", true, "online", true));
    }

    // ============================================================
    // OFFLINE
    // ============================================================
    @PostMapping("/offline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> offline() {
        Long uid = usuarioActual.id();
        if (uid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "online", false));
        }
        presenciaService.marcarDesconectado(uid);
        log.debug("Offline para usuario {}", uid);
        return ResponseEntity.ok(Map.of("ok", true, "online", false));
    }

    // ============================================================
    // CONSULTAR PRESENCIA DE UN USUARIO
    // ============================================================
    @GetMapping("/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerPresencia(@PathVariable Long usuarioId) {
        if (usuarioId == null || usuarioId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "ID de usuario invalido"));
        }

        boolean enLinea = presenciaService.estaEnLinea(usuarioId);
        Instant ultima = presenciaService.obtenerUltimaConexion(usuarioId);

        return ResponseEntity.ok(Map.of(
                "usuarioId", usuarioId,
                "enLinea", enLinea,
                "ultimaConexion", ultima != null ? ultima.toString() : ""
        ));
    }

    // ============================================================
    // CONSULTAR PRESENCIA DE VARIOS USUARIOS
    // ============================================================
    @PostMapping("/multiple")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerPresenciaMultiple(
            @RequestBody(required = false) Map<String, List<Long>> body) {

        if (body == null || body.get("usuarioIds") == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Lista de usuarioIds requerida"));
        }

        List<Long> ids = body.get("usuarioIds");

        if (ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // SEGURIDAD: limitar el tamano para evitar DoS contra Redis
        if (ids.size() > MAX_IDS_CONSULTA_MULTIPLE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Maximo " + MAX_IDS_CONSULTA_MULTIPLE + " usuarios por consulta"));
        }

        Map<Long, PresenciaService.EstadoPresencia> estados = presenciaService.obtenerEstados(ids);

        List<Map<String, Object>> result = estados.entrySet().stream()
                .map(entry -> {
                    Long uid = entry.getKey();
                    PresenciaService.EstadoPresencia estado = entry.getValue();
                    return Map.<String, Object>of(
                            "usuarioId", uid,
                            "enLinea", estado.enLinea(),
                            "ultimaConexion", estado.ultimaConexion() != null
                                    ? estado.ultimaConexion().toString()
                                    : ""
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}