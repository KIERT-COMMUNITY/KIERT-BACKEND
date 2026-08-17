package com.kiert.backend.controller;

import com.kiert.backend.dto.ReaccionDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.ReaccionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reacciones")
@RequiredArgsConstructor
public class ReaccionController {

    private final ReaccionService reaccionService;
    private final UsuarioActual usuarioActual;

    @PostMapping("/post/{postId}")
    public ResponseEntity<Map<String, Long>> reaccionarPost(
            @PathVariable Long postId,
            @RequestBody ReaccionDTO datos) {
        log.info("Reaccionando a post {} con tipo {}", postId, datos.tipo());
        Map<String, Long> resultado = reaccionService.reaccionarPost(usuarioActual.id(), postId, datos.tipo());
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/comentario/{comentarioId}")
    public ResponseEntity<Map<String, Long>> reaccionarComentario(
            @PathVariable Long comentarioId,
            @RequestBody ReaccionDTO datos) {
        log.info("Reaccionando a comentario {} con tipo {}", comentarioId, datos.tipo());
        Map<String, Long> resultado = reaccionService.reaccionarComentario(usuarioActual.id(), comentarioId, datos.tipo());
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<Map<String, Long>> obtenerReaccionesPost(@PathVariable Long postId) {
        return ResponseEntity.ok(reaccionService.obtenerReaccionesPost(postId));
    }

    @GetMapping("/comentario/{comentarioId}")
    public ResponseEntity<Map<String, Long>> obtenerReaccionesComentario(@PathVariable Long comentarioId) {
        return ResponseEntity.ok(reaccionService.obtenerReaccionesComentario(comentarioId));
    }

    @GetMapping("/post/{postId}/usuario")
    public ResponseEntity<Boolean> usuarioReaccionoPost(@PathVariable Long postId) {
        return ResponseEntity.ok(reaccionService.usuarioReaccionoPost(usuarioActual.id(), postId));
    }
}