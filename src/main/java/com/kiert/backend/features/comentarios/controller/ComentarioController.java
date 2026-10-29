// src/main/java/com/kiert/backend/features/comentarios/controller/ComentarioController.java
package com.kiert.backend.features.comentarios.controller;

import com.kiert.backend.features.comentarios.dto.ComentarioDTO;
import com.kiert.backend.features.respuesta.dto.RespuestaDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.comentarios.service.ComentarioService;
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
@RequestMapping("/api/comentarios")
@RequiredArgsConstructor
public class ComentarioController {

    private final ComentarioService comentarioService;
    private final UsuarioActual usuarioActual;

    // ========== COMENTARIOS ==========

    @GetMapping("/post/{postId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ComentarioDTO>> listarPorPost(@PathVariable Long postId) {
        log.debug("Listando comentarios del post: {}", postId);
        return ResponseEntity.ok(comentarioService.listarPorPost(postId));
    }

    @PostMapping("/post/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crearComentario(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {

        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        String contenido = body != null ? body.get("contenido") : null;
        if (contenido == null || contenido.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El contenido es obligatorio"));
        }

        log.info("Creando comentario en post: {}, usuario: {}", postId, usuarioId);
        return ResponseEntity.ok(comentarioService.crearComentario(postId, usuarioId, contenido));
    }

    @DeleteMapping("/{comentarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarComentario(@PathVariable Long comentarioId) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        log.info("Eliminando comentario: {}", comentarioId);
        comentarioService.eliminarComentario(comentarioId, usuarioId);
        return ResponseEntity.ok(Map.of("mensaje", "Comentario eliminado"));
    }

    // ========== RESPUESTAS ==========

    @GetMapping("/{comentarioId}/respuestas")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RespuestaDTO>> listarRespuestas(@PathVariable Long comentarioId) {
        log.debug("Listando respuestas del comentario: {}", comentarioId);
        return ResponseEntity.ok(comentarioService.listarRespuestas(comentarioId));
    }

    @PostMapping("/{comentarioId}/respuestas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crearRespuesta(
            @PathVariable Long comentarioId,
            @RequestBody Map<String, String> body) {

        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        String contenido = body != null ? body.get("contenido") : null;
        if (contenido == null || contenido.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El contenido es obligatorio"));
        }

        log.info("Creando respuesta al comentario: {}, usuario: {}", comentarioId, usuarioId);
        return ResponseEntity.ok(comentarioService.crearRespuesta(comentarioId, usuarioId, contenido));
    }

    @DeleteMapping("/respuestas/{respuestaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarRespuesta(@PathVariable Long respuestaId) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        log.info("Eliminando respuesta: {}", respuestaId);
        comentarioService.eliminarRespuesta(respuestaId, usuarioId);
        return ResponseEntity.ok(Map.of("mensaje", "Respuesta eliminada"));
    }

    // ========== REACCIONES ==========

    @PostMapping("/{comentarioId}/reaccionar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reaccionarComentario(
            @PathVariable Long comentarioId,
            @RequestBody Map<String, String> body) {

        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        String tipo = body != null ? body.get("tipo") : null;
        if (tipo == null || tipo.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El tipo de reaccion es obligatorio"));
        }

        log.info("Reaccionando a comentario: {}, tipo: {}, usuario: {}", comentarioId, tipo, usuarioId);
        return ResponseEntity.ok(comentarioService.reaccionarComentario(comentarioId, usuarioId, tipo));
    }

    @PostMapping("/respuestas/{respuestaId}/reaccionar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> reaccionarRespuesta(
            @PathVariable Long respuestaId,
            @RequestBody Map<String, String> body) {

        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        String tipo = body != null ? body.get("tipo") : null;
        if (tipo == null || tipo.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El tipo de reaccion es obligatorio"));
        }

        log.info("Reaccionando a respuesta: {}, tipo: {}, usuario: {}", respuestaId, tipo, usuarioId);
        return ResponseEntity.ok(comentarioService.reaccionarRespuesta(respuestaId, usuarioId, tipo));
    }
}