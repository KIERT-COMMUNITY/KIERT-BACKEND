// src/main/java/com/kiert/backend/features/sticker/controller/StickerController.java
package com.kiert.backend.features.sticker.controller;

import com.kiert.backend.features.sticker.dto.StickerDTO;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.sticker.service.StickerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stickers")
@RequiredArgsConstructor
public class StickerController {

    private final StickerService stickerService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // LISTAR MIS STICKERS
    // ============================================================
    @GetMapping("/mis-stickers")
    @PreAuthorize("isAuthenticated()")
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
            log.warn("Error al listar stickers del usuario {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al listar stickers"));
        }
    }

    // ============================================================
    // LISTAR STICKERS PUBLICOS
    // ============================================================
    @GetMapping("/publicos")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> stickersPublicos() {
        try {
            List<StickerDTO> stickers = stickerService.listarStickersPublicos();
            return ResponseEntity.ok(stickers);
        } catch (Exception e) {
            log.warn("Error al listar stickers publicos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al listar stickers publicos"));
        }
    }

    // ============================================================
    // CREAR STICKER
    // ============================================================
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crearSticker(
            @RequestParam("nombre") String nombre,
            @RequestParam(value = "esPublico", required = false, defaultValue = "false") Boolean esPublico,
            @RequestParam("archivo") MultipartFile archivo) {

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

        log.debug("POST /api/stickers - usuario={} nombre='{}' esPublico={}",
                usuarioId, nombre, esPublico);

        try {
            StickerDTO creado = stickerService.crearSticker(
                    usuarioId, nombre.trim(), esPublico, archivo
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);

        } catch (IllegalArgumentException e) {
            log.warn("Validacion fallida al crear sticker por usuario {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (RecursoNoEncontradoException e) {
            log.warn("Recurso no encontrado al crear sticker: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));

        } catch (SecurityException e) {
            log.warn("Acceso denegado al crear sticker: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso"));

        } catch (Exception e) {
            log.warn("Error al crear sticker para usuario {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo crear el sticker"));
        }
    }

    // ============================================================
    // ELIMINAR STICKER
    // ============================================================
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
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
                    .body(Map.of("error", "No tienes permiso para eliminar este sticker"));

        } catch (RecursoNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sticker no encontrado"));

        } catch (Exception e) {
            log.warn("Error al eliminar sticker {} por usuario {}: {}", id, usuarioId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo eliminar el sticker"));
        }
    }
}