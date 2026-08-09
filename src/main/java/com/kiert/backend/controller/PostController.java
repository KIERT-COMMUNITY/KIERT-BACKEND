package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Espejo exacto de las rutas que llama post.service.ts en el frontend
// (base: environment.apiUrl + "/publicaciones").
@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<List<PostDTO>> listar() {
        return ResponseEntity.ok(postService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(postService.obtenerPorId(id));
    }

    // Recibe multipart/form-data: create-post.component.ts arma un FormData con
    // titulo, categoria, descripcion, link (opcional) y archivos[] (opcional).
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PostDTO> crear(
            @RequestParam String titulo,
            @RequestParam String categoria,
            @RequestParam String descripcion,
            @RequestParam(required = false) String link,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos
    ) {
        CrearPostDTO datos = new CrearPostDTO(titulo, categoria, descripcion, link);
        PostDTO creado = postService.crear(usuarioActual.id(), datos, archivos);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{postId}/comentarios")
    public ResponseEntity<List<ComentarioDTO>> listarComentarios(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.listarComentarios(postId));
    }

    @PostMapping("/{postId}/comentarios")
    public ResponseEntity<ComentarioDTO> comentar(@PathVariable Long postId,
                                                    @Valid @RequestBody CrearComentarioDTO datos) {
        ComentarioDTO comentario = postService.comentar(postId, usuarioActual.id(), datos.contenido());
        return ResponseEntity.status(HttpStatus.CREATED).body(comentario);
    }
}
