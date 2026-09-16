// src/main/java/com/kiert/backend/controller/NotificacionController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.NotificacionDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<NotificacionDTO>> listar() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(notificacionService.obtenerNotificaciones(usuarioId));
    }

    @GetMapping("/no-leidas/count")
    public ResponseEntity<Long> contarNoLeidas() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(notificacionService.contarNoLeidas(usuarioId));
    }

    @PutMapping("/leer")
    public ResponseEntity<?> marcarLeidas(@RequestBody Map<String, List<Long>> body) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        notificacionService.marcarComoLeidas(body.get("ids"), usuarioId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/leer-todas")
    public ResponseEntity<?> marcarTodasLeidas() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        notificacionService.marcarTodasComoLeidas(usuarioId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        notificacionService.eliminarNotificacion(id, usuarioId);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}