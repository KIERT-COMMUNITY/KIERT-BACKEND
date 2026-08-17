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

    @GetMapping
    @Operation(summary = "Listar todas las publicaciones", description = "Obtiene todas las publicaciones de la comunidad ordenadas por fecha de creación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de publicaciones obtenida exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<PostDTO>> listar() {
        log.info("📋 Recibiendo solicitud para listar posts");
        return ResponseEntity.ok(postService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener publicación por ID", description = "Obtiene los detalles de una publicación específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Publicación encontrada"),
            @ApiResponse(responseCode = "404", description = "Publicación no encontrada")
    })
    public ResponseEntity<PostDTO> obtenerPorId(
            @Parameter(description = "ID de la publicación", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(postService.obtenerPorId(id));
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Crear nueva publicación", description = "Crea una nueva publicación con archivos adjuntos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Publicación creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<PostDTO> crear(
            @Parameter(description = "Título de la publicación", required = true)
            @RequestParam String titulo,
            @Parameter(description = "Categoría de la publicación", required = true)
            @RequestParam String categoria,
            @Parameter(description = "Descripción de la publicación", required = true)
            @RequestParam String descripcion,
            @Parameter(description = "Link externo opcional")
            @RequestParam(required = false) String link,
            @Parameter(description = "Archivos adjuntos (imágenes, PDFs, etc.)")
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {

        log.info("📝 Creando post para usuario: {}", usuarioActual.id());

        CrearPostDTO datos = new CrearPostDTO(titulo, categoria, descripcion, link);
        PostDTO creado = postService.crear(usuarioActual.id(), datos, archivos);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{postId}/comentarios")
    @Operation(summary = "Listar comentarios de una publicación", description = "Obtiene todos los comentarios de una publicación")
    public ResponseEntity<List<ComentarioDTO>> listarComentarios(
            @Parameter(description = "ID de la publicación", required = true)
            @PathVariable Long postId) {
        return ResponseEntity.ok(postService.listarComentarios(postId));
    }

    @PostMapping("/{postId}/comentarios")
    @Operation(summary = "Agregar comentario", description = "Agrega un comentario a una publicación")
    public ResponseEntity<ComentarioDTO> comentar(
            @Parameter(description = "ID de la publicación", required = true)
            @PathVariable Long postId,
            @Valid @RequestBody CrearComentarioDTO datos) {
        ComentarioDTO comentario = postService.comentar(postId, usuarioActual.id(), datos.contenido());
        return ResponseEntity.status(HttpStatus.CREATED).body(comentario);
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @Operation(summary = "Actualizar publicación", description = "Actualiza los campos de una publicación y gestiona sus adjuntos (solo el autor)")
    public ResponseEntity<PostDTO> actualizarPost(
            @Parameter(description = "ID de la publicación", required = true)
            @PathVariable Long id,
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String link,
            @RequestParam(value = "adjuntosEliminar", required = false) List<Long> adjuntosEliminar,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {

        ActualizarPostDTO datos = new ActualizarPostDTO(titulo, descripcion, categoria, link);
        PostDTO actualizado = postService.actualizarPost(id, usuarioActual.id(), datos, adjuntosEliminar, archivos);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar publicación", description = "Elimina una publicación (solo el autor)")
    public ResponseEntity<Void> eliminarPost(
            @Parameter(description = "ID de la publicación", required = true)
            @PathVariable Long id) {
        postService.eliminarPost(id, usuarioActual.id());
        return ResponseEntity.noContent().build();
    }
}