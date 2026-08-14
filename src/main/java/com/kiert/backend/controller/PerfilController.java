package com.kiert.backend.controller;

import com.kiert.backend.dto.ActualizarFotoPerfilDTO;
import com.kiert.backend.dto.UsuarioDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.PerfilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // ========== OBTENER PERFIL ==========
    @GetMapping
    public ResponseEntity<UsuarioDTO> obtenerPerfil() {
        log.info("📋 Obteniendo perfil del usuario: {}", usuarioActual.id());
        return ResponseEntity.ok(perfilService.obtenerPerfil(usuarioActual.id()));
    }

    // ========== SUBIR FOTO DE PERFIL (MULTIPART) ==========
    @PostMapping(value = "/foto", consumes = "multipart/form-data")
    public ResponseEntity<UsuarioDTO> subirFoto(@RequestParam("archivo") MultipartFile archivo) {
        log.info("📸 Subiendo foto para usuario: {}", usuarioActual.id());
        UsuarioDTO usuario = perfilService.subirFotoPerfil(usuarioActual.id(), archivo);
        return ResponseEntity.ok(usuario);
    }

    // ========== ACTUALIZAR FOTO DE PERFIL (URL) ==========
    @PatchMapping("/foto-url")
    public ResponseEntity<UsuarioDTO> actualizarFotoUrl(@RequestBody ActualizarFotoPerfilDTO datos) {
        log.info("📸 Actualizando foto de perfil del usuario: {}", usuarioActual.id());
        UsuarioDTO usuario = perfilService.actualizarFotoPerfil(usuarioActual.id(), datos.urlFoto());
        return ResponseEntity.ok(usuario);
    }

    // ========== LIMPIAR CACHÉ ==========
    @DeleteMapping("/cache")
    public ResponseEntity<Void> limpiarCache() {
        log.info("🧹 Limpiando caché del usuario: {}", usuarioActual.id());
        perfilService.eliminarCachePerfil(usuarioActual.id());
        return ResponseEntity.ok().build();
    }
}