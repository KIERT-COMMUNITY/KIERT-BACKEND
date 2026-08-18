package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
@Tag(name = "Publicaciones", description = "Endpoints para gestionar publicaciones de la comunidad")
@SecurityRequirement(name = "bearerAuth")
public class PostController {

    private final PostService postService;
    private final UsuarioActual usuarioActual;

    // ========== LISTAR POSTS ==========
    @GetMapping
    @Operation(summary = "Listar todas las publicaciones")
    public ResponseEntity<List<PostDTO>> listar() {
        log.info("📋 Recibiendo solicitud para listar posts");
        return ResponseEntity.ok(postService.listar());
    }

    // ========== OBTENER POST POR ID ==========
    @GetMapping("/{id}")
    @Operation(summary = "Obtener publicación por ID")
    public ResponseEntity<PostDTO> obtenerPorId(@PathVariable Long id) {
        log.info("🔍 Obteniendo post: {}", id);
        return ResponseEntity.ok(postService.obtenerPorId(id));
    }

    // ========== CREAR POST CON ARCHIVOS ==========
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Crear nueva publicación")
    public ResponseEntity<PostDTO> crearPost(
            @RequestParam("titulo") String titulo,
            @RequestParam("categoria") String categoria,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "link", required = false) String link,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {

        log.info("📝 Creando post para usuario: {}", usuarioActual.id());
        log.info("📄 Título: {}", titulo);
        log.info("📎 Archivos recibidos: {}", archivos != null ? archivos.size() : 0);

        if (archivos != null && !archivos.isEmpty()) {
            for (MultipartFile archivo : archivos) {
                log.info("📎 Archivo: {}, Tamaño: {} bytes, Tipo: {}",
                        archivo.getOriginalFilename(),
                        archivo.getSize(),
                        archivo.getContentType());
            }
        }

        CrearPostDTO datos = new CrearPostDTO(titulo, categoria, descripcion, link);
        PostDTO creado = postService.crear(usuarioActual.id(), datos, archivos);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // ========== ELIMINAR POST ==========
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar publicación")
    public ResponseEntity<Void> eliminarPost(@PathVariable Long id) {
        log.info("🗑️ Eliminando post: {}", id);
        postService.eliminarPost(id, usuarioActual.id());
        return ResponseEntity.noContent().build();
    }

    // ========== LISTAR COMENTARIOS ==========
    @GetMapping("/{postId}/comentarios")
    @Operation(summary = "Listar comentarios de una publicación")
    public ResponseEntity<List<ComentarioDTO>> listarComentarios(@PathVariable Long postId) {
        log.info("💬 Listando comentarios del post: {}", postId);
        return ResponseEntity.ok(postService.listarComentarios(postId));
    }

    // ========== CREAR COMENTARIO ==========
    @PostMapping("/{postId}/comentarios")
    @Operation(summary = "Agregar comentario")
    public ResponseEntity<ComentarioDTO> comentar(
            @PathVariable Long postId,
            @Valid @RequestBody CrearComentarioDTO datos) {
        log.info("💬 Creando comentario en post: {}, usuario: {}", postId, usuarioActual.id());
        ComentarioDTO comentario = postService.comentar(postId, usuarioActual.id(), datos.contenido());
        return ResponseEntity.status(HttpStatus.CREATED).body(comentario);
    }
}