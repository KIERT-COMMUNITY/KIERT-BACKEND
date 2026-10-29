// src/main/java/com/kiert/backend/features/compartidos/controller/CompartidoController.java
package com.kiert.backend.features.compartidos.controller;

import com.kiert.backend.features.compartidos.dto.CompartidoDTO;
import com.kiert.backend.features.post.dto.CompartirPostDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.compartidos.service.CompartidoService;
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
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class CompartidoController {

    private final CompartidoService compartidoService;
    private final UsuarioActual usuarioActual;

    /**
     * Compartir un post
     * POST /api/publicaciones/{postId}/compartir
     */
    @PostMapping("/{postId}/compartir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> compartir(
            @PathVariable Long postId,
            @RequestBody(required = false) CompartirPostDTO dto) {

        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            CompartirPostDTO data = dto != null ? dto : new CompartirPostDTO("INTERNO", null);
            CompartidoDTO compartido = compartidoService.compartir(usuarioId, postId, data);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Publicacion compartida correctamente",
                    "compartido", compartido
            ));

        } catch (Exception e) {
            log.error("Error al compartir post {} por usuario {}: {}",
                    postId, usuarioId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo compartir la publicacion"));
        }
    }

    /**
     * Ver quienes compartieron un post
     * GET /api/publicaciones/{postId}/compartidos
     */
    @GetMapping("/{postId}/compartidos")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<CompartidoDTO>> listarCompartidos(@PathVariable Long postId) {
        return ResponseEntity.ok(compartidoService.listarPorPost(postId));
    }

    /**
     * Contar compartidos de un post
     * GET /api/publicaciones/{postId}/compartidos/count
     */
    @GetMapping("/{postId}/compartidos/count")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Long>> contarCompartidos(@PathVariable Long postId) {
        return ResponseEntity.ok(Map.of("total", compartidoService.contarCompartidos(postId)));
    }
}