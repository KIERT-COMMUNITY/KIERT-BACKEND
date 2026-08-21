package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.PersonalizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/personalizacion")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PersonalizacionController {

    private final PersonalizacionService personalizacionService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public ResponseEntity<?> obtenerPersonalizacion() {
        try {
            Long usuarioId = usuarioActual.id();
            log.info("🔍 UsuarioActual.id() = {}", usuarioId);

            if (usuarioId == null) {
                log.error("❌ Usuario no autenticado en obtenerPersonalizacion");
                // ✅ Devolver un DTO por defecto en lugar de error
                return ResponseEntity.ok(new PersonalizacionDTO(
                        null, null, "default", "none", "default", null, null, null
                ));
            }

            log.info("📋 Obteniendo personalización del usuario: {}", usuarioId);
            PersonalizacionDTO resultado = personalizacionService.obtenerPersonalizacion(usuarioId);

            if (resultado == null) {
                log.warn("⚠️ Personalización null, devolviendo default");
                return ResponseEntity.ok(new PersonalizacionDTO(
                        null, usuarioId, "default", "none", "default", null, null, null
                ));
            }

            log.info("✅ Personalización obtenida: ID={}, tema={}, marco={}, fondo={}",
                    resultado.id(), resultado.temaId(), resultado.marcoId(), resultado.fondoId());
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error en obtenerPersonalizacion: {}", e.getMessage(), e);
            // ✅ Devolver un DTO por defecto en lugar de error 500
            return ResponseEntity.ok(new PersonalizacionDTO(
                    null, null, "default", "none", "default", null, null, null
            ));
        }
    }

    @PutMapping
    public ResponseEntity<?> guardarPersonalizacion(@RequestBody PersonalizacionDTO datos) {
        try {
            Long usuarioId = usuarioActual.id();
            log.info("🔍 UsuarioActual.id() = {}", usuarioId);

            if (usuarioId == null) {
                log.error("❌ Usuario no autenticado en guardarPersonalizacion");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }

            log.info("💾 Guardando personalización del usuario: {}", usuarioId);
            log.info("📦 Datos recibidos: tema={}, marco={}, fondo={}",
                    datos.temaId(), datos.marcoId(), datos.fondoId());

            PersonalizacionDTO resultado = personalizacionService.guardarPersonalizacion(
                    usuarioId, datos.temaId(), datos.marcoId(), datos.fondoId());

            log.info("✅ Personalización guardada: ID={}, tema={}, marco={}, fondo={}",
                    resultado.id(), resultado.temaId(), resultado.marcoId(), resultado.fondoId());
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error en guardarPersonalizacion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar personalización: " + e.getMessage());
        }
    }

    @GetMapping("/marcos")
    public ResponseEntity<?> obtenerMarcos() {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                log.error("❌ Usuario no autenticado en obtenerMarcos");
                return ResponseEntity.ok(personalizacionService.obtenerMarcos(1L));
            }
            log.info("📋 Obteniendo marcos para usuario: {}", usuarioId);
            return ResponseEntity.ok(personalizacionService.obtenerMarcos(usuarioId));
        } catch (Exception e) {
            log.error("❌ Error en obtenerMarcos: {}", e.getMessage(), e);
            return ResponseEntity.ok(personalizacionService.obtenerMarcos(1L));
        }
    }

    @GetMapping("/fondos")
    public ResponseEntity<?> obtenerFondos() {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                log.error("❌ Usuario no autenticado en obtenerFondos");
                return ResponseEntity.ok(personalizacionService.obtenerFondos(1L));
            }
            log.info("📋 Obteniendo fondos para usuario: {}", usuarioId);
            return ResponseEntity.ok(personalizacionService.obtenerFondos(usuarioId));
        } catch (Exception e) {
            log.error("❌ Error en obtenerFondos: {}", e.getMessage(), e);
            return ResponseEntity.ok(personalizacionService.obtenerFondos(1L));
        }
    }

    @PostMapping("/foto-perfil")
    public ResponseEntity<?> subirFotoPerfil(@RequestParam("archivo") MultipartFile archivo) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                log.error("❌ Usuario no autenticado en subirFotoPerfil");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }
            log.info("📸 Subiendo foto de perfil para usuario: {}", usuarioId);
            return ResponseEntity.ok(personalizacionService.subirFotoPerfil(usuarioId, archivo));
        } catch (Exception e) {
            log.error("❌ Error en subirFotoPerfil: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir foto: " + e.getMessage());
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<?> obtenerPersonalizacionPorUsuario(@PathVariable Long usuarioId) {
        try {
            log.info("📋 Obteniendo personalización del usuario: {}", usuarioId);
            PersonalizacionDTO resultado = personalizacionService.obtenerPersonalizacion(usuarioId);

            if (resultado == null) {
                log.warn("⚠️ Personalización null para usuario {}, devolviendo default", usuarioId);
                return ResponseEntity.ok(new PersonalizacionDTO(
                        null, usuarioId, "default", "none", "default", null, null, null
                ));
            }

            log.info("✅ Personalización del usuario {} obtenida: tema={}, marco={}, fondo={}",
                    usuarioId, resultado.temaId(), resultado.marcoId(), resultado.fondoId());
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error en obtenerPersonalizacionPorUsuario para {}: {}", usuarioId, e.getMessage(), e);
            return ResponseEntity.ok(new PersonalizacionDTO(
                    null, usuarioId, "default", "none", "default", null, null, null
            ));
        }
    }
}