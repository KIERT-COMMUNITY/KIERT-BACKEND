package com.kiert.backend.controller;

import com.kiert.backend.dto.BloqueoDTO;
import com.kiert.backend.dto.CrearBloqueoDTO;
import com.kiert.backend.dto.EstadoBloqueoDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.BloqueoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bloqueos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class BloqueoController {

    private final BloqueoService bloqueoService;
    private final UsuarioActual usuarioActual;

    /**
     * Bloquear usuario
     * POST /api/bloqueos
     */
    @PostMapping
    public ResponseEntity<?> bloquear(@RequestBody CrearBloqueoDTO dto) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            BloqueoDTO bloqueo = bloqueoService.bloquear(usuarioId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Usuario bloqueado correctamente",
                    "bloqueo", bloqueo
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error al bloquear usuario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al bloquear usuario"));
        }
    }

    /**
     * Desbloquear usuario
     * DELETE /api/bloqueos/{usuarioId}
     */
    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<?> desbloquear(@PathVariable Long usuarioId) {
        try {
            Long usuarioActualId = usuarioActual.id();
            if (usuarioActualId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            bloqueoService.desbloquear(usuarioActualId, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario desbloqueado correctamente"));
        } catch (Exception e) {
            log.error("❌ Error al desbloquear usuario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verificar estado de bloqueo
     * GET /api/bloqueos/estado/{usuarioId}
     */
    @GetMapping("/estado/{usuarioId}")
    public ResponseEntity<EstadoBloqueoDTO> verificarEstado(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bloqueoService.verificarEstado(usuarioActualId, usuarioId));
    }

    /**
     * Listar usuarios bloqueados
     * GET /api/bloqueos
     */
    @GetMapping
    public ResponseEntity<List<BloqueoDTO>> listarBloqueados() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bloqueoService.listarBloqueados(usuarioId));
    }
}