// src/main/java/com/kiert/backend/controller/PresenciaController.java
package com.kiert.backend.features.presencia.controller;

import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.presencia.service.PresenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/presencia")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PresenciaController {

    private final PresenciaService presenciaService;
    private final UsuarioActual usuarioActual;

    // src/main/java/com/kiert/backend/controller/PresenciaController.java
// Reemplaza SOLO el método heartbeat

    @PostMapping("/heartbeat")
    public ResponseEntity<?> heartbeat() {
        Long uid = usuarioActual.id();
        if (uid == null) {
            log.warn("⚠️ Heartbeat sin usuario autenticado (esto NO debería pasar si el filtro JWT funciona)");
            return ResponseEntity.status(401).body(Map.of("ok", false, "online", false));
        }
        presenciaService.marcarEnLinea(uid);
        log.info("💓 Heartbeat OK para usuario {}", uid);
        return ResponseEntity.ok(Map.of("ok", true, "online", true));
    }

    @PostMapping("/offline")
    public ResponseEntity<?> offline() {
        Long uid = usuarioActual.id();
        if (uid == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "online", false));
        }
        presenciaService.marcarDesconectado(uid);
        log.info("🔴 Offline para usuario {}", uid);
        return ResponseEntity.ok(Map.of("ok", true, "online", false));
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<?> obtenerPresencia(@PathVariable Long usuarioId) {
        boolean enLinea = presenciaService.estaEnLinea(usuarioId);
        Instant ultima = presenciaService.obtenerUltimaConexion(usuarioId);

        return ResponseEntity.ok(Map.of(
                "usuarioId", usuarioId,
                "enLinea", enLinea,
                "ultimaConexion", ultima != null ? ultima.toString() : ""
        ));
    }

    @PostMapping("/multiple")
    public ResponseEntity<List<Map<String, Object>>> obtenerPresenciaMultiple(
            @RequestBody Map<String, List<Long>> body) {

        List<Long> ids = body.get("usuarioIds");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
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