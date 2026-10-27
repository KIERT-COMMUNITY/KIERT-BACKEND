// src/main/java/com/kiert/backend/features/post/controller/PostController.java
package com.kiert.backend.features.post.controller;

import com.kiert.backend.entity.Post;
import com.kiert.backend.features.post.dto.CrearPostDTO;
import com.kiert.backend.features.post.dto.PostDTO;
import com.kiert.backend.features.post.repository.PostRepository;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.post.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PostController {

    private final PostService postService;
    private final PostRepository postRepository;
    private final UsuarioActual usuarioActual;

    @GetMapping("/diagnostico")
    public ResponseEntity<?> diagnosticar() {
        try {
            List<Post> posts = postRepository.findAll();
            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Post p : posts) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("titulo", p.getTitulo());
                item.put("categoria", p.getCategoria());
                item.put("eliminado", p.isEliminado());
                item.put("esAnonimo", p.getEsAnonimo());
                item.put("autorId", p.getAutor() != null ? p.getAutor().getId() : null);
                resultado.add(item);
            }
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error en diagnostico: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en diagnostico: " + e.getMessage());
        }
    }

    @GetMapping("/activos")
    public ResponseEntity<?> listarActivos() {
        try {
            List<Post> posts = postRepository.findAllActiveOrderByFechaCreacionDesc();
            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Post p : posts) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("titulo", p.getTitulo());
                item.put("descripcion", p.getDescripcion());
                item.put("categoria", p.getCategoria());
                item.put("esAnonimo", p.getEsAnonimo());
                item.put("fechaCreacion", p.getFechaCreacion().toString());
                resultado.add(item);
            }
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error al listar posts activos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            List<PostDTO> posts = postService.listar();
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            log.error("Error al listar posts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al cargar publicaciones: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().body("ID de publicacion invalido");
            }
            PostDTO post = postService.obtenerPorId(id);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            log.error("Error al obtener post {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener publicacion: " + e.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CacheEvict(value = {"publicaciones", "busquedaUsuarios", "posts", "post"}, allEntries = true)
    public ResponseEntity<?> crearPost(
            @RequestParam("titulo") String titulo,
            @RequestParam("categoria") String categoria,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "link", required = false) String link,
            @RequestParam(value = "esAnonimo", required = false, defaultValue = "false") Boolean esAnonimo,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {

        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }

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
                        .body("Error: el post no obtuvo ID");
            }

            log.info("Post creado: id={}, anonimo={}", creado.id(), creado.esAnonimo());
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);

        } catch (Exception e) {
            log.error("Error al crear post: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear publicacion: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = {"publicaciones", "busquedaUsuarios", "posts", "post"}, allEntries = true)
    public ResponseEntity<?> eliminarPost(@PathVariable Long id) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }
            postService.eliminarPost(id, usuarioId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al eliminar post {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar publicacion: " + e.getMessage());
        }
    }
}