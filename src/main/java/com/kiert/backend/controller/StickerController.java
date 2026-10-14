// src/main/java/com/kiert/backend/controller/StickerController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.StickerDTO;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.StickerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stickers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class StickerController {

    private final StickerService stickerService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // LISTAR MIS STICKERS
    // ============================================================
    @GetMapping("/mis-stickers")
    public ResponseEntity<?> misStickers() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            List<StickerDTO> stickers = stickerService.listarMisStickers(usuarioId);
            return ResponseEntity.ok(stickers);
        } catch (Exception e) {
            log.error("Error al listar stickers del usuario {}: {}", usuarioId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al listar stickers"));
        }
    }

    // ============================================================
    // LISTAR STICKERS PÚBLICOS
    // ============================================================
    @GetMapping("/publicos")
    public ResponseEntity<?> stickersPublicos() {
        try {
            List<StickerDTO> stickers = stickerService.listarStickersPublicos();
            return ResponseEntity.ok(stickers);
        } catch (Exception e) {
            log.error("Error al listar stickers públicos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al listar stickers públicos"));
        }
    }

    // ============================================================
    // CREAR STICKER
    // ============================================================
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> crearSticker(
            @RequestParam("nombre") String nombre,
            @RequestParam(value = "esPublico", required = false, defaultValue = "false") Boolean esPublico,
            @RequestParam("archivo") MultipartFile archivo) {

        // ✅ Validación temprana
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        if (archivo == null || archivo.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo del sticker es obligatorio"));
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El nombre del sticker es obligatorio"));
        }

        log.info("POST /api/stickers — usuario={} nombre='{}' esPublico={} archivo={} ({} KB)",
                usuarioId, nombre, esPublico,
                archivo.getOriginalFilename(), archivo.getSize() / 1024);

        try {
            StickerDTO creado = stickerService.crearSticker(
                    usuarioId, nombre.trim(), esPublico, archivo
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);

        } catch (IllegalArgumentException e) {
            // Error de validación (formato, tamaño, etc.) → 400
            log.warn("Validación fallida al crear sticker: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (RecursoNoEncontradoException e) {
            // Usuario no encontrado → 404
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));

        } catch (SecurityException e) {
            // Sin permisos → 403
            log.warn("Acceso denegado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));

        } catch (RuntimeException e) {
            // Error al subir a Cloudinary u otro → 500 con mensaje claro
            log.error("Error al crear sticker para usuario {}: {}", usuarioId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            // Cualquier otra cosa → 500 genérico
            log.error("Error inesperado al crear sticker: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error inesperado al crear el sticker"));
        }
    }

    // ============================================================
    // ELIMINAR STICKER
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarSticker(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            stickerService.eliminarSticker(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Sticker eliminado"));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));

        } catch (RecursoNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Error al eliminar sticker {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}