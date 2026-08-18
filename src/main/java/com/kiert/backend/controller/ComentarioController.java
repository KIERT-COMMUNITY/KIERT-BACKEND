package com.kiert.backend.controller;

import com.kiert.backend.dto.ComentarioDTO;
import com.kiert.backend.dto.RespuestaDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.ComentarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<ComentarioDTO>> listarPorPost(@PathVariable Long postId) {
        log.info("📋 Listando comentarios del post: {}", postId);
        return ResponseEntity.ok(comentarioService.listarPorPost(postId));
    }

    // ✅ Endpoint para crear comentario SOLO CON TEXTO (sin imagen)
    @PostMapping("/post/{postId}")
    public ResponseEntity<ComentarioDTO> crearComentario(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        String contenido = body.get("contenido");
        log.info("📝 Creando comentario en post: {}, usuario: {}, contenido: {}", postId, usuarioActual.id(), contenido);
        return ResponseEntity.ok(comentarioService.crearComentario(postId, usuarioActual.id(), contenido));
    }

    @DeleteMapping("/{comentarioId}")
    public ResponseEntity<Void> eliminarComentario(@PathVariable Long comentarioId) {
        log.info("🗑️ Eliminando comentario: {}", comentarioId);
        comentarioService.eliminarComentario(comentarioId, usuarioActual.id());
        return ResponseEntity.ok().build();
    }

    // ========== RESPUESTAS ==========

    @GetMapping("/{comentarioId}/respuestas")
    public ResponseEntity<List<RespuestaDTO>> listarRespuestas(@PathVariable Long comentarioId) {
        log.info("📋 Listando respuestas del comentario: {}", comentarioId);
        return ResponseEntity.ok(comentarioService.listarRespuestas(comentarioId));
    }

    // ✅ Endpoint para crear respuesta SOLO CON TEXTO (sin imagen)
    @PostMapping("/{comentarioId}/respuestas")
    public ResponseEntity<RespuestaDTO> crearRespuesta(
            @PathVariable Long comentarioId,
            @RequestBody Map<String, String> body) {
        String contenido = body.get("contenido");
        log.info("📝 Creando respuesta al comentario: {}, usuario: {}", comentarioId, usuarioActual.id());
        return ResponseEntity.ok(comentarioService.crearRespuesta(comentarioId, usuarioActual.id(), contenido));
    }

    @DeleteMapping("/respuestas/{respuestaId}")
    public ResponseEntity<Void> eliminarRespuesta(@PathVariable Long respuestaId) {
        log.info("🗑️ Eliminando respuesta: {}", respuestaId);
        comentarioService.eliminarRespuesta(respuestaId, usuarioActual.id());
        return ResponseEntity.ok().build();
    }

    // ========== REACCIONES ==========

    @PostMapping("/{comentarioId}/reaccionar")
    public ResponseEntity<Map<String, Long>> reaccionarComentario(
            @PathVariable Long comentarioId,
            @RequestBody Map<String, String> body) {
        String tipo = body.get("tipo");
        log.info("❤️ Reaccionando a comentario: {}, tipo: {}, usuario: {}", comentarioId, tipo, usuarioActual.id());
        return ResponseEntity.ok(comentarioService.reaccionarComentario(comentarioId, usuarioActual.id(), tipo));
    }

    @PostMapping("/respuestas/{respuestaId}/reaccionar")
    public ResponseEntity<Map<String, Long>> reaccionarRespuesta(
            @PathVariable Long respuestaId,
            @RequestBody Map<String, String> body) {
        String tipo = body.get("tipo");
        log.info("❤️ Reaccionando a respuesta: {}, tipo: {}, usuario: {}", respuestaId, tipo, usuarioActual.id());
        return ResponseEntity.ok(comentarioService.reaccionarRespuesta(respuestaId, usuarioActual.id(), tipo));
    }
}