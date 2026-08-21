package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.Post;
import com.kiert.backend.repository.PostRepository;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.PostService;
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

    // ✅ ENDPOINT DE DIAGNÓSTICO - OBTENER POSTS CRUDOS
    @GetMapping("/diagnostico")
    public ResponseEntity<?> diagnosticar() {
        try {
            log.info("🔍 Diagnóstico: Obteniendo posts crudos");
            List<Post> posts = postRepository.findAll();
            log.info("📊 Total posts en BD: {}", posts.size());

            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Post p : posts) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("titulo", p.getTitulo());
                item.put("categoria", p.getCategoria());
                item.put("eliminado", p.isEliminado());
                item.put("autorId", p.getAutor() != null ? p.getAutor().getId() : null);
                resultado.add(item);
            }

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error en diagnóstico: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en diagnóstico: " + e.getMessage());
        }
    }

    // ✅ ENDPOINT DE PRUEBA - POSTS ACTIVOS SIMPLES
    @GetMapping("/activos")
    public ResponseEntity<?> listarActivos() {
        try {
            log.info("📋 Listando posts activos (sin DTO)");
            List<Post> posts = postRepository.findAllActiveOrderByFechaCreacionDesc();
            log.info("✅ Se encontraron {} posts activos", posts.size());

            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Post p : posts) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("titulo", p.getTitulo());
                item.put("descripcion", p.getDescripcion());
                item.put("categoria", p.getCategoria());
                item.put("fechaCreacion", p.getFechaCreacion().toString());
                resultado.add(item);
            }

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error al listar posts activos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // ✅ ENDPOINT ORIGINAL CON LOGS MEJORADOS
    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            log.info("📋 Recibiendo solicitud para listar posts");

            long count = postRepository.count();
            log.info("📊 Total posts en BD: {}", count);

            List<PostDTO> posts = postService.listar();
            log.info("✅ Se devolvieron {} posts", posts.size());

            if (!posts.isEmpty()) {
                posts.forEach(p -> log.info("📌 Post ID: {}, Categoría: {}", p.id(), p.categoria()));
            }

            return ResponseEntity.ok(posts);

        } catch (Exception e) {
            log.error("❌ Error al listar posts: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al cargar publicaciones: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            log.info("🔍 Obteniendo post: {}", id);

            if (id == null || id <= 0) {
                log.warn("⚠️ ID inválido: {}", id);
                return ResponseEntity.badRequest().body("ID de publicación inválido");
            }

            PostDTO post = postService.obtenerPorId(id);
            log.info("✅ Post encontrado: ID={}, Categoría={}", post.id(), post.categoria());
            return ResponseEntity.ok(post);

        } catch (Exception e) {
            log.error("❌ Error al obtener post {}: {}", id, e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener publicación: " + e.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CacheEvict(value = {"publicaciones", "busquedaUsuarios"}, allEntries = true)
    public ResponseEntity<?> crearPost(
            @RequestParam("titulo") String titulo,
            @RequestParam("categoria") String categoria,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "link", required = false) String link,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {

        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                log.error("❌ Usuario no autenticado en crearPost");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }

            log.info("📝 Creando post para usuario: {}", usuarioId);
            log.info("📄 Título: {}", titulo);
            log.info("📂 Categoría: '{}'", categoria);
            log.info("📝 Descripción: {}", descripcion);
            log.info("🔗 Link: {}", link);
            log.info("📎 Archivos: {}", archivos != null ? archivos.size() : 0);

            CrearPostDTO datos = new CrearPostDTO(titulo, categoria, descripcion, link);
            PostDTO creado = postService.crear(usuarioId, datos, archivos);

            log.info("✅ Post creado exitosamente con ID: {}, Categoría: '{}'", creado.id(), creado.categoria());
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);

        } catch (Exception e) {
            log.error("❌ Error al crear post: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear publicación: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = {"publicaciones", "busquedaUsuarios"}, allEntries = true)
    public ResponseEntity<?> eliminarPost(@PathVariable Long id) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                log.error("❌ Usuario no autenticado en eliminarPost");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }
            log.info("🗑️ Eliminando post: {} por usuario {}", id, usuarioId);
            postService.eliminarPost(id, usuarioId);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("❌ Error al eliminar post {}: {}", id, e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar publicación: " + e.getMessage());
        }
    }
}