// src/main/java/com/kiert/backend/controller/PerfilController.java
package com.kiert.backend.features.perfil.controller;

import com.kiert.backend.features.perfil.dto.ActualizarFotoPerfilDTO;
import com.kiert.backend.features.usuario.dto.UsuarioDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.perfil.service.PerfilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // OBTENER PERFIL
    // ============================================================
    @GetMapping
    @Cacheable(value = "perfil", key = "#usuarioActual.id()")
    public ResponseEntity<UsuarioDTO> obtenerPerfil() {
        log.info("Obteniendo perfil del usuario: {}", usuarioActual.id());
        return ResponseEntity.ok(perfilService.obtenerPerfil(usuarioActual.id()));
    }

    // ============================================================
    // SUBIR FOTO (multipart)
    // ============================================================
    @PostMapping(value = "/foto", consumes = "multipart/form-data")
    @CacheEvict(value = {"perfil", "usuarios", "personalizacion"}, allEntries = true)
    public ResponseEntity<UsuarioDTO> subirFoto(@RequestParam("archivo") MultipartFile archivo) {
        log.info("Subiendo foto para usuario: {}", usuarioActual.id());
        UsuarioDTO usuario = perfilService.subirFotoPerfil(usuarioActual.id(), archivo);
        return ResponseEntity.ok(usuario);
    }

    // ============================================================
    // ACTUALIZAR FOTO POR URL
    // ============================================================
    @PatchMapping("/foto-url")
    @CacheEvict(value = {"perfil", "usuarios", "personalizacion"}, allEntries = true)
    public ResponseEntity<UsuarioDTO> actualizarFotoUrl(@RequestBody ActualizarFotoPerfilDTO datos) {
        log.info("Actualizando foto de perfil del usuario: {}", usuarioActual.id());
        UsuarioDTO usuario = perfilService.actualizarFotoPerfil(usuarioActual.id(), datos.urlFoto());
        return ResponseEntity.ok(usuario);
    }

    // ============================================================
    // ✅ ELIMINAR FOTO DE PERFIL
    // ============================================================
    @DeleteMapping("/foto")
    @CacheEvict(value = {"perfil", "usuarios", "personalizacion"}, allEntries = true)
    public ResponseEntity<UsuarioDTO> eliminarFoto() {
        log.info("Eliminando foto de perfil del usuario: {}", usuarioActual.id());
        UsuarioDTO usuario = perfilService.eliminarFotoPerfil(usuarioActual.id());
        return ResponseEntity.ok(usuario);
    }

    // ============================================================
    // LIMPIAR CACHÉ
    // ============================================================
    @DeleteMapping("/cache")
    @CacheEvict(value = {"perfil", "usuarios", "publicaciones", "busquedaUsuarios", "contactos", "solicitudes"}, allEntries = true)
    public ResponseEntity<Void> limpiarCache() {
        log.info("Limpiando caché del usuario: {}", usuarioActual.id());
        perfilService.eliminarCachePerfil(usuarioActual.id());
        return ResponseEntity.ok().build();
    }
}