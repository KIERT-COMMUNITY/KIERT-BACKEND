// src/main/java/com/kiert/backend/features/post/controller/PostController.java
package com.kiert.backend.features.post.controller;

import com.kiert.backend.features.post.dto.CrearPostDTO;
import com.kiert.backend.features.post.dto.PostDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.post.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // LISTAR POSTS
    // ============================================================
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> listar() {
        try {
            List<PostDTO> posts = postService.listar();
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            log.warn("Error al listar posts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudieron cargar las publicaciones"));
        }
    }

    // ============================================================
    // OBTENER POST POR ID
    // ============================================================
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "ID de publicacion invalido"));
        }
        try {
            PostDTO post = postService.obtenerPorId(id);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            log.warn("Error al obtener post {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Publicacion no encontrada"));
        }
    }

    // ============================================================
    // CREAR POST
    // ============================================================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crearPost(
            @RequestParam("titulo") String titulo,
            @RequestParam("categoria") String categoria,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "link", required = false) String link,
            @RequestParam(value = "esAnonimo", required = false, defaultValue = "false") Boolean esAnonimo,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {

        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            CrearPostDTO datos = new CrearPostDTO(
                    titulo,
                    categoria,
                    descripcion,
                    link,
                    esAnonimo != null ? esAnonimo : false
            );

            PostDTO creado = postService.crear(usuarioId, datos, archivos);

            if (creado == null || creado.id() == null) {
                log.error("El post se creo pero no tiene ID valido");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "No se pudo crear la publicacion"));
            }

            log.info("Post creado: id={}, anonimo={}", creado.id(), creado.esAnonimo());
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);

        } catch (Exception e) {
            log.warn("Error al crear post por usuario {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo crear la publicacion"));
        }
    }

    // ============================================================
    // ELIMINAR POST
    // ============================================================
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarPost(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            postService.eliminarPost(id, usuarioId);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para eliminar esta publicacion"));
        } catch (Exception e) {
            log.warn("Error al eliminar post {} por usuario {}: {}", id, usuarioId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo eliminar la publicacion"));
        }
    }
}