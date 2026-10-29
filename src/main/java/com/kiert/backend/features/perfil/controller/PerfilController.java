// src/main/java/com/kiert/backend/features/perfil/controller/PerfilController.java
package com.kiert.backend.features.perfil.controller;

import com.kiert.backend.features.perfil.dto.ActualizarFotoPerfilDTO;
import com.kiert.backend.features.usuario.dto.UsuarioDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.perfil.service.PerfilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PerfilController {

    private final PerfilService perfilService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // OBTENER PERFIL
    // ============================================================
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerPerfil() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Usuario no autenticado"));
        }
        log.debug("Obteniendo perfil del usuario: {}", usuarioId);
        return ResponseEntity.ok(perfilService.obtenerPerfil(usuarioId));
    }

    // ============================================================
    // SUBIR FOTO (multipart)
    // ============================================================
    @PostMapping(value = "/foto", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> subirFoto(@RequestParam("archivo") MultipartFile archivo) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Usuario no autenticado"));
        }
        log.info("Subiendo foto para usuario: {}", usuarioId);
        return ResponseEntity.ok(perfilService.subirFotoPerfil(usuarioId, archivo));
    }

    // ============================================================
    // ACTUALIZAR FOTO POR URL
    // ============================================================
    @PatchMapping("/foto-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> actualizarFotoUrl(@RequestBody ActualizarFotoPerfilDTO datos) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Usuario no autenticado"));
        }
        if (datos == null || datos.urlFoto() == null || datos.urlFoto().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "URL de foto requerida"));
        }
        log.info("Actualizando foto de perfil del usuario: {}", usuarioId);
        return ResponseEntity.ok(perfilService.actualizarFotoPerfil(usuarioId, datos.urlFoto()));
    }

    // ============================================================
    // ELIMINAR FOTO DE PERFIL
    // ============================================================
    @DeleteMapping("/foto")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarFoto() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Usuario no autenticado"));
        }
        log.info("Eliminando foto de perfil del usuario: {}", usuarioId);
        return ResponseEntity.ok(perfilService.eliminarFotoPerfil(usuarioId));
    }

    // ============================================================
    // LIMPIAR CACHE (solo desarrollo)
    // ============================================================
    @DeleteMapping("/cache")
    @PreAuthorize("isAuthenticated()")
    @Profile("dev")
    public ResponseEntity<Void> limpiarCache() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Limpiando cache del usuario: {}", usuarioId);
        perfilService.eliminarCachePerfil(usuarioId);
        return ResponseEntity.ok().build();
    }
}