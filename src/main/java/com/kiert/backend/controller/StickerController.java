// src/main/java/com/kiert/backend/controller/StickerController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.StickerDTO;
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

    @GetMapping("/mis-stickers")
    public ResponseEntity<List<StickerDTO>> misStickers() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(stickerService.listarMisStickers(usuarioId));
    }

    @GetMapping("/publicos")
    public ResponseEntity<List<StickerDTO>> stickersPublicos() {
        return ResponseEntity.ok(stickerService.listarStickersPublicos());
    }

    @PostMapping
    public ResponseEntity<?> crearSticker(
            @RequestParam("nombre") String nombre,
            @RequestParam(value = "esPublico", required = false, defaultValue = "false") Boolean esPublico,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(stickerService.crearSticker(usuarioId, nombre, esPublico, archivo));
        } catch (Exception e) {
            log.error("Error al crear sticker: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarSticker(@PathVariable Long id) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            stickerService.eliminarSticker(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Sticker eliminado"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}