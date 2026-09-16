// src/main/java/com/kiert/backend/controller/GrupoChatController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.GrupoChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class GrupoChatController {

    private final GrupoChatService grupoService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // CREAR GRUPO
    // ============================================================
    @PostMapping
    public ResponseEntity<?> crearGrupo(@RequestBody CrearGrupoDTO dto) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return ResponseEntity.status(HttpStatus.CREATED).body(grupoService.crearGrupo(usuarioId, dto));
        } catch (Exception e) {
            log.error("❌ Error al crear grupo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // LISTAR
    // ============================================================
    @GetMapping("/mis-grupos")
    public ResponseEntity<List<GrupoDTO>> misGrupos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(grupoService.listarMisGrupos(usuarioId));
    }

    @GetMapping("/publicos")
    public ResponseEntity<List<GrupoDTO>> gruposPublicos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(grupoService.listarGruposPublicos(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerGrupo(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(grupoService.obtenerGrupo(id, usuarioId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // MIEMBROS
    // ============================================================
    @GetMapping("/{id}/miembros")
    public ResponseEntity<List<MiembroGrupoDTO>> listarMiembros(@PathVariable Long id) {
        log.info("👥 GET /api/grupos/{}/miembros", id);
        return ResponseEntity.ok(grupoService.listarMiembros(id));
    }

    // ============================================================
    // MENSAJES
    // ============================================================
    @GetMapping("/{id}/mensajes")
    public ResponseEntity<List<MensajeGrupoDTO>> obtenerMensajes(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        return ResponseEntity.ok(grupoService.obtenerMensajes(id, usuarioId));
    }

    @PostMapping("/{id}/mensajes")
    public ResponseEntity<?> enviarMensaje(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            String contenido = body.get("contenido");
            if (contenido == null || contenido.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mensaje vacío"));
            }

            return ResponseEntity.ok(grupoService.enviarMensaje(id, usuarioId, contenido));
        } catch (Exception e) {
            log.error("❌ Error al enviar mensaje: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // INVITACIONES
    // ============================================================
    @PostMapping("/{id}/invitar")
    public ResponseEntity<?> invitarUsuarios(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> body) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            List<Long> usuariosIds = body.get("usuariosIds");
            grupoService.invitarUsuarios(id, usuarioId, usuariosIds);
            return ResponseEntity.ok(Map.of("mensaje", "Invitaciones enviadas"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/aceptar")
    public ResponseEntity<?> aceptarInvitacion(@PathVariable Long id) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            grupoService.aceptarInvitacion(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Te uniste al grupo"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarInvitacion(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        grupoService.rechazarInvitacion(id, usuarioId);
        return ResponseEntity.ok(Map.of("mensaje", "Invitación rechazada"));
    }

    @PostMapping("/{id}/unirse")
    public ResponseEntity<?> unirseAGrupo(@PathVariable Long id) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            grupoService.unirseAGrupoPublico(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Te uniste al grupo"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/salir")
    public ResponseEntity<?> salirDelGrupo(@PathVariable Long id) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            grupoService.salirDelGrupo(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Saliste del grupo"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // 🔥 ELIMINAR GRUPO (solo creador o ADMIN)
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarGrupo(@PathVariable Long id) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            grupoService.eliminarGrupo(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Grupo eliminado correctamente"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error al eliminar grupo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // 🔥 EXPULSAR MIEMBRO (solo ADMIN)
    // ============================================================
    @DeleteMapping("/{id}/miembros/{usuarioId}")
    public ResponseEntity<?> expulsarMiembro(
            @PathVariable Long id,
            @PathVariable Long usuarioId) {
        try {
            Long adminId = usuarioActual.id();
            if (adminId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            grupoService.expulsarMiembro(id, adminId, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Miembro expulsado correctamente"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error al expulsar miembro: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // INVITACIONES PENDIENTES
    // ============================================================
    @GetMapping("/invitaciones")
    public ResponseEntity<List<InvitacionGrupoDTO>> invitacionesPendientes() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(grupoService.listarInvitacionesPendientes(usuarioId));
    }
}