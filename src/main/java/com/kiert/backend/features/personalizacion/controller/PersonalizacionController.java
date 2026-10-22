// src/main/java/com/kiert/backend/features/personalizacion/controller/PersonalizacionController.java
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
    public ResponseEntity<PersonalizacionDTO> obtenerPersonalizacion() {
        Long usuarioId = usuarioActual.id();
        log.debug("UsuarioActual.id() = {}", usuarioId);

        if (usuarioId == null) {
            log.warn("Usuario no autenticado, devolviendo default");
            return ResponseEntity.ok(defaultDTO(null, null));
        }

        // ✅ Llama al método cacheable que crea si no existe y cachea correctamente
        PersonalizacionDTO resultado = personalizacionService.obtenerOCrearPersonalizacion(usuarioId);

        if (resultado == null) {
            return ResponseEntity.ok(defaultDTO(usuarioId, null));
        }
        return ResponseEntity.ok(resultado);
    }

    // ============================================================
    // GUARDAR PERSONALIZACIÓN
    // ============================================================
    @PutMapping
    public ResponseEntity<?> guardarPersonalizacion(@RequestBody PersonalizacionDTO datos) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        log.info("Guardando personalización del usuario: {}", usuarioId);
        PersonalizacionDTO resultado = personalizacionService.guardarPersonalizacion(
                usuarioId, datos.temaId(), datos.marcoId(), datos.fondoId());

        return ResponseEntity.ok(resultado);
    }

    // ============================================================
    // MARCOS
    // ============================================================
    @GetMapping("/marcos")
    public ResponseEntity<?> obtenerMarcos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.ok(personalizacionService.obtenerMarcos(1L));
        }
        return ResponseEntity.ok(personalizacionService.obtenerMarcos(usuarioId));
    }

    // ============================================================
    // FONDOS
    // ============================================================
    @GetMapping("/fondos")
    public ResponseEntity<?> obtenerFondos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.ok(personalizacionService.obtenerFondos(1L));
        }
        return ResponseEntity.ok(personalizacionService.obtenerFondos(usuarioId));
    }

    // ============================================================
    // SUBIR FOTO DE PERFIL
    // ============================================================
    @PostMapping("/foto-perfil")
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
    @CacheEvict(value = {"personalizacion", "perfil", "usuarios"}, allEntries = true)
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
    // OBTENER PERSONALIZACIÓN DE OTRO USUARIO
    // ============================================================
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<PersonalizacionDTO> obtenerPersonalizacionPorUsuario(@PathVariable Long usuarioId) {
        log.debug("Obteniendo personalización del usuario: {}", usuarioId);
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