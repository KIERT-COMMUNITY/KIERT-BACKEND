// src/main/java/com/kiert/backend/features/personalizacion/controller/PersonalizacionController.java
package com.kiert.backend.features.personalizacion.controller;

import com.kiert.backend.features.personalizacion.dto.PersonalizacionDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.personalizacion.service.PersonalizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/personalizacion")
@RequiredArgsConstructor
public class PersonalizacionController {

    private final PersonalizacionService personalizacionService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // OBTENER PERSONALIZACION DEL USUARIO ACTUAL
    // ============================================================
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerPersonalizacion() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        PersonalizacionDTO resultado = personalizacionService.obtenerOCrearPersonalizacion(usuarioId);
        if (resultado == null) {
            return ResponseEntity.ok(defaultDTO(usuarioId, null));
        }
        return ResponseEntity.ok(resultado);
    }

    // ============================================================
    // GUARDAR PERSONALIZACION
    // ============================================================
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> guardarPersonalizacion(@RequestBody(required = false) PersonalizacionDTO datos) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        if (datos == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Datos de personalizacion requeridos"));
        }

        log.info("Guardando personalizacion del usuario: {}", usuarioId);
        PersonalizacionDTO resultado = personalizacionService.guardarPersonalizacion(
                usuarioId, datos.temaId(), datos.marcoId(), datos.fondoId());

        return ResponseEntity.ok(resultado);
    }

    // ============================================================
    // MARCOS
    // ============================================================
    @GetMapping("/marcos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerMarcos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        return ResponseEntity.ok(personalizacionService.obtenerMarcos(usuarioId));
    }

    // ============================================================
    // FONDOS
    // ============================================================
    @GetMapping("/fondos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerFondos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        return ResponseEntity.ok(personalizacionService.obtenerFondos(usuarioId));
    }

    // ============================================================
    // SUBIR FOTO DE PERFIL
    // ============================================================
    @PostMapping("/foto-perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> subirFotoPerfil(@RequestParam("archivo") MultipartFile archivo) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        log.info("Subiendo foto de perfil para usuario: {}", usuarioId);
        return ResponseEntity.ok(personalizacionService.subirFotoPerfil(usuarioId, archivo));
    }

    // ============================================================
    // ELIMINAR FOTO DE PERFIL
    // ============================================================
    @DeleteMapping("/foto-perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarFotoPerfil() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        log.info("Eliminando foto de perfil del usuario: {}", usuarioId);
        PersonalizacionDTO resultado = personalizacionService.eliminarFotoPerfil(usuarioId);

        return ResponseEntity.ok(resultado);
    }

    // ============================================================
    // OBTENER PERSONALIZACION DE OTRO USUARIO
    // ============================================================
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerPersonalizacionPorUsuario(@PathVariable Long usuarioId) {
        if (usuarioId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuario ID requerido"));
        }
        log.debug("Obteniendo personalizacion del usuario: {}", usuarioId);
        PersonalizacionDTO resultado = personalizacionService.obtenerPersonalizacionPorUsuario(usuarioId);

        if (resultado == null) {
            return ResponseEntity.ok(defaultDTO(usuarioId, null));
        }
        return ResponseEntity.ok(resultado);
    }

    // ============================================================
    // HELPER
    // ============================================================
    private PersonalizacionDTO defaultDTO(Long usuarioId, Long id) {
        return new PersonalizacionDTO(
                id, usuarioId, "default", "none", "default", null, null, null
        );
    }
}