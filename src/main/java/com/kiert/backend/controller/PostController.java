package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<PostDTO>> listar() {
        log.info("📋 Recibiendo solicitud para listar posts");
        try {
            List<PostDTO> posts = postService.listar();
            log.info("✅ Devolviendo {} posts", posts.size());
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            log.error("❌ Error al listar posts: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(postService.obtenerPorId(id));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PostDTO> crear(
            @RequestParam String titulo,
            @RequestParam String categoria,
            @RequestParam String descripcion,
            @RequestParam(required = false) String link,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {

        log.info("📝 Creando post para usuario: {}", usuarioActual.id());

        CrearPostDTO datos = new CrearPostDTO(titulo, categoria, descripcion, link);
        PostDTO creado = postService.crear(usuarioActual.id(), datos, archivos);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{postId}/comentarios")
    public ResponseEntity<List<ComentarioDTO>> listarComentarios(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.listarComentarios(postId));
    }

    @PostMapping("/{postId}/comentarios")
    public ResponseEntity<ComentarioDTO> comentar(
            @PathVariable Long postId,
            @Valid @RequestBody CrearComentarioDTO datos) {
        ComentarioDTO comentario = postService.comentar(postId, usuarioActual.id(), datos.contenido());
        return ResponseEntity.status(HttpStatus.CREATED).body(comentario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPost(@PathVariable Long id) {
        postService.eliminarPost(id, usuarioActual.id());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostDTO> actualizarPost(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPostDTO datos) {
        return ResponseEntity.ok(postService.actualizarPost(id, usuarioActual.id(), datos));
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Void> limpiarCachePosts() {
        postService.limpiarCachePosts();
        return ResponseEntity.ok().build();
    }
}