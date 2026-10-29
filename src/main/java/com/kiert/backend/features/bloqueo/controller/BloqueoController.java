// src/main/java/com/kiert/backend/features/bloqueo/controller/BloqueoController.java
package com.kiert.backend.features.bloqueo.controller;

import com.kiert.backend.features.bloqueo.dto.BloqueoDTO;
import com.kiert.backend.features.bloqueo.dto.CrearBloqueoDTO;
import com.kiert.backend.features.bloqueo.dto.EstadoBloqueoDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.bloqueo.service.BloqueoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bloqueos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BloqueoController {

    private final BloqueoService bloqueoService;
    private final UsuarioActual usuarioActual;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> bloquear(@RequestBody CrearBloqueoDTO dto) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            BloqueoDTO bloqueo = bloqueoService.bloquear(usuarioId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Usuario bloqueado correctamente. Ya no podra enviarte mensajes.",
                    "bloqueo", bloqueo
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al bloquear usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al bloquear usuario"));
        }
    }

    @DeleteMapping("/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> desbloquear(@PathVariable Long usuarioId) {
        try {
            Long usuarioActualId = usuarioActual.id();
            if (usuarioActualId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            bloqueoService.desbloquear(usuarioActualId, usuarioId);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Usuario desbloqueado correctamente. Ya pueden volver a comunicarse."
            ));
        } catch (Exception e) {
            log.error("Error al desbloquear usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al desbloquear usuario"));
        }
    }

    @GetMapping("/estado/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EstadoBloqueoDTO> verificarEstado(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bloqueoService.verificarEstado(usuarioActualId, usuarioId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BloqueoDTO>> listarBloqueados() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bloqueoService.listarBloqueados(usuarioId));
    }
}