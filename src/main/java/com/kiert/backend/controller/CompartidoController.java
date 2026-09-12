package com.kiert.backend.controller;

import com.kiert.backend.dto.CompartidoDTO;
import com.kiert.backend.dto.CompartirPostDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.CompartidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CompartidoController {

    private final CompartidoService compartidoService;
    private final UsuarioActual usuarioActual;

    /**
     * Compartir un post
     * POST /api/publicaciones/{postId}/compartir
     */
    @PostMapping("/{postId}/compartir")
    public ResponseEntity<?> compartir(
            @PathVariable Long postId,
            @RequestBody(required = false) CompartirPostDTO dto) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            CompartirPostDTO data = dto != null ? dto : new CompartirPostDTO("INTERNO", null);
            CompartidoDTO compartido = compartidoService.compartir(usuarioId, postId, data);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Publicación compartida correctamente",
                    "compartido", compartido
            ));

        } catch (Exception e) {
            log.error("❌ Error al compartir: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Ver quiénes compartieron un post
     * GET /api/publicaciones/{postId}/compartidos
     */
    @GetMapping("/{postId}/compartidos")
    public ResponseEntity<List<CompartidoDTO>> listarCompartidos(@PathVariable Long postId) {
        return ResponseEntity.ok(compartidoService.listarPorPost(postId));
    }

    /**
     * Contar compartidos de un post
     * GET /api/publicaciones/{postId}/compartidos/count
     */
    @GetMapping("/{postId}/compartidos/count")
    public ResponseEntity<Map<String, Long>> contarCompartidos(@PathVariable Long postId) {
        return ResponseEntity.ok(Map.of("total", compartidoService.contarCompartidos(postId)));
    }
}