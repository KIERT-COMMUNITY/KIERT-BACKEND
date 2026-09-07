// src/main/java/com/kiert/backend/controller/NotificationController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.NotificacionDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<NotificacionDTO>> obtenerNotificaciones() {
        Long usuarioId = usuarioActual.id();
        log.info("📋 Obteniendo notificaciones para usuario: {}", usuarioId);
        return ResponseEntity.ok(notificationService.obtenerNotificaciones(usuarioId));
    }

    @GetMapping("/no-leidas/count")
    public ResponseEntity<Long> contarNoLeidas() {
        Long usuarioId = usuarioActual.id();
        log.info("🔔 Contando notificaciones no leídas para usuario: {}", usuarioId);
        return ResponseEntity.ok(notificationService.contarNoLeidas(usuarioId));
    }

    @PutMapping("/leer")
    public ResponseEntity<Void> marcarComoLeidas(@RequestBody Map<String, List<Long>> body) {
        Long usuarioId = usuarioActual.id();
        List<Long> ids = body.get("ids");
        log.info("✅ Marcando como leídas {} notificaciones para usuario: {}", ids.size(), usuarioId);
        notificationService.marcarComoLeidas(ids, usuarioId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodasComoLeidas() {
        Long usuarioId = usuarioActual.id();
        log.info("✅ Marcando todas las notificaciones como leídas para usuario: {}", usuarioId);
        notificationService.marcarTodasComoLeidas(usuarioId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarNotificacion(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        log.info("🗑️ Eliminando notificación: {} para usuario: {}", id, usuarioId);
        notificationService.eliminarNotificacion(id, usuarioId);
        return ResponseEntity.ok().build();
    }
}