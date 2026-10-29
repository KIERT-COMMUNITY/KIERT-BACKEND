// src/main/java/com/kiert/backend/features/reacciones/controller/ReaccionController.java
package com.kiert.backend.features.reacciones.controller;

import com.kiert.backend.features.reacciones.dto.ReaccionDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.reacciones.service.ReaccionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reacciones")
@RequiredArgsConstructor
public class ReaccionController {

    private final ReaccionService reaccionService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // REACCIONAR A POST
    // ============================================================
    @PostMapping("/post/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reaccionarPost(
            @PathVariable Long postId,
            @RequestBody(required = false) ReaccionDTO datos) {

        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        if (datos == null || datos.tipo() == null || datos.tipo().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo de reaccion requerido"));
        }

        log.debug("Reaccionando a post {} con tipo {}", postId, datos.tipo());
        Map<String, Long> resultado = reaccionService.reaccionarPost(usuarioId, postId, datos.tipo());
        return ResponseEntity.ok(resultado);
    }

    // ============================================================
    // REACCIONAR A COMENTARIO
    // ============================================================
    @PostMapping("/comentario/{comentarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reaccionarComentario(
            @PathVariable Long comentarioId,
            @RequestBody(required = false) ReaccionDTO datos) {

        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        if (datos == null || datos.tipo() == null || datos.tipo().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo de reaccion requerido"));
        }

        log.debug("Reaccionando a comentario {} con tipo {}", comentarioId, datos.tipo());
        Map<String, Long> resultado = reaccionService.reaccionarComentario(usuarioId, comentarioId, datos.tipo());
        return ResponseEntity.ok(resultado);
    }

    // ============================================================
    // OBTENER REACCIONES DE POST
    // ============================================================
    @GetMapping("/post/{postId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Long>> obtenerReaccionesPost(@PathVariable Long postId) {
        return ResponseEntity.ok(reaccionService.obtenerReaccionesPost(postId));
    }

    // ============================================================
    // OBTENER REACCIONES DE COMENTARIO
    // ============================================================
    @GetMapping("/comentario/{comentarioId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Long>> obtenerReaccionesComentario(@PathVariable Long comentarioId) {
        return ResponseEntity.ok(reaccionService.obtenerReaccionesComentario(comentarioId));
    }

    // ============================================================
    // SABER SI USUARIO REACCIONO A POST
    // ============================================================
    @GetMapping("/post/{postId}/usuario")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> usuarioReaccionoPost(@PathVariable Long postId) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        return ResponseEntity.ok(reaccionService.usuarioReaccionoPost(usuarioId, postId));
    }
}