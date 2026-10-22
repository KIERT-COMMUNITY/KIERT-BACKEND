// src/main/java/com/kiert/backend/controller/PersonalizacionController.java
package com.kiert.backend.features.personalizacion.controller;

import com.kiert.backend.features.personalizacion.dto.PersonalizacionDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.personalizacion.service.PersonalizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/personalizacion")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PersonalizacionController {

    private final PersonalizacionService personalizacionService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // OBTENER PERSONALIZACIÓN DEL USUARIO ACTUAL
    // ============================================================
    @GetMapping
    public ResponseEntity<?> obtenerPersonalizacion() {
        try {
            Long usuarioId = usuarioActual.id();
            log.info("UsuarioActual.id() = {}", usuarioId);

            if (usuarioId == null) {
                log.warn("Usuario no autenticado, devolviendo default");
                return ResponseEntity.ok(new PersonalizacionDTO(
                        null, null, "default", "none", "default", null, null, null
                ));
            }

            PersonalizacionDTO resultado = personalizacionService.obtenerPersonalizacion(usuarioId);

            if (resultado == null) {
                return ResponseEntity.ok(new PersonalizacionDTO(
                        null, usuarioId, "default", "none", "default", null, null, null
                ));
            }

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("Error en obtenerPersonalizacion: {}", e.getMessage(), e);
            return ResponseEntity.ok(new PersonalizacionDTO(
                    null, null, "default", "none", "default", null, null, null
            ));
        }
    }

    // ============================================================
    // GUARDAR PERSONALIZACIÓN
    // ============================================================
    @PutMapping
    public ResponseEntity<?> guardarPersonalizacion(@RequestBody PersonalizacionDTO datos) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            log.info("Guardando personalización del usuario: {}", usuarioId);
            PersonalizacionDTO resultado = personalizacionService.guardarPersonalizacion(
                    usuarioId, datos.temaId(), datos.marcoId(), datos.fondoId());

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("Error en guardarPersonalizacion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar personalización: " + e.getMessage()));
        }
    }

    // ============================================================
    // MARCOS
    // ============================================================
    @GetMapping("/marcos")
    public ResponseEntity<?> obtenerMarcos() {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.ok(personalizacionService.obtenerMarcos(1L));
            }
            return ResponseEntity.ok(personalizacionService.obtenerMarcos(usuarioId));
        } catch (Exception e) {
            log.error("Error en obtenerMarcos: {}", e.getMessage(), e);
            return ResponseEntity.ok(personalizacionService.obtenerMarcos(1L));
        }
    }

    // ============================================================
    // FONDOS
    // ============================================================
    @GetMapping("/fondos")
    public ResponseEntity<?> obtenerFondos() {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.ok(personalizacionService.obtenerFondos(1L));
            }
            return ResponseEntity.ok(personalizacionService.obtenerFondos(usuarioId));
        } catch (Exception e) {
            log.error("Error en obtenerFondos: {}", e.getMessage(), e);
            return ResponseEntity.ok(personalizacionService.obtenerFondos(1L));
        }
    }

    // ============================================================
    // SUBIR FOTO DE PERFIL
    // ============================================================
    @PostMapping("/foto-perfil")
    public ResponseEntity<?> subirFotoPerfil(@RequestParam("archivo") MultipartFile archivo) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }
            log.info("Subiendo foto de perfil para usuario: {}", usuarioId);
            return ResponseEntity.ok(personalizacionService.subirFotoPerfil(usuarioId, archivo));
        } catch (Exception e) {
            log.error("Error en subirFotoPerfil: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al subir foto: " + e.getMessage()));
        }
    }

    // ============================================================
    // ✅ NUEVO: ELIMINAR FOTO DE PERFIL
    // ============================================================
    @DeleteMapping("/foto-perfil")
    @CacheEvict(value = {"personalizacion", "perfil", "usuarios"}, allEntries = true)
    public ResponseEntity<?> eliminarFotoPerfil() {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            log.info("Eliminando foto de perfil del usuario: {}", usuarioId);
            PersonalizacionDTO resultado = personalizacionService.eliminarFotoPerfil(usuarioId);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("Error al eliminar foto de perfil: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar foto: " + e.getMessage()));
        }
    }

    // ============================================================
    // OBTENER PERSONALIZACIÓN DE OTRO USUARIO
    // ============================================================
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> obtenerPersonalizacionPorUsuario(@PathVariable Long usuarioId) {
        try {
            log.info("Obteniendo personalización del usuario: {}", usuarioId);
            PersonalizacionDTO resultado = personalizacionService.obtenerPersonalizacion(usuarioId);

            if (resultado == null) {
                return ResponseEntity.ok(new PersonalizacionDTO(
                        null, usuarioId, "default", "none", "default", null, null, null
                ));
            }

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("Error en obtenerPersonalizacionPorUsuario para {}: {}", usuarioId, e.getMessage(), e);
            return ResponseEntity.ok(new PersonalizacionDTO(
                    null, usuarioId, "default", "none", "default", null, null, null
            ));
        }
    }
}