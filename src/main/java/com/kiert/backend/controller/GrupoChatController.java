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
import org.springframework.web.multipart.MultipartFile;

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
            log.error("Error al crear grupo: {}", e.getMessage(), e);
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
        log.info("GET /api/grupos/{}/miembros", id);
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
            log.error("Error al enviar mensaje: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // MENSAJE CON ARCHIVO
    // ============================================================
    @PostMapping("/{id}/mensajes/con-archivo")
    public ResponseEntity<?> enviarMensajeConArchivo(
            @PathVariable Long id,
            @RequestParam(value = "contenido", required = false) String contenido,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            if ((contenido == null || contenido.trim().isEmpty()) && (archivo == null || archivo.isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mensaje o archivo requerido"));
            }

            return ResponseEntity.ok(grupoService.enviarMensajeConArchivo(id, usuarioId, contenido, archivo));
        } catch (Exception e) {
            log.error("Error al enviar mensaje con archivo: {}", e.getMessage(), e);
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
    // ELIMINAR GRUPO
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
            log.error("Error al eliminar grupo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // EXPULSAR MIEMBRO
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
            log.error("Error al expulsar miembro: {}", e.getMessage(), e);
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

    // ============================================================
    // FOTO DEL GRUPO
    // ============================================================
    @PostMapping("/{id}/foto")
    public ResponseEntity<?> actualizarFoto(
            @PathVariable Long id,
            @RequestParam("foto") MultipartFile foto) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            return ResponseEntity.ok(grupoService.actualizarFotoGrupo(id, usuarioId, foto));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al subir foto: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/foto")
    public ResponseEntity<?> eliminarFoto(@PathVariable Long id) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            return ResponseEntity.ok(grupoService.eliminarFotoGrupo(id, usuarioId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // EDITAR INFO DEL GRUPO
    // ============================================================
    @PatchMapping("/{id}/info")
    public ResponseEntity<?> actualizarInfo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            String nombre = body.get("nombre");
            String descripcion = body.get("descripcion");

            return ResponseEntity.ok(grupoService.actualizarInfoGrupo(id, usuarioId, nombre, descripcion));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // INVITACIONES POR LINK
    // ============================================================
    @PostMapping("/{id}/invitacion-link")
    public ResponseEntity<?> generarLinkInvitacion(
            @PathVariable Long id,
            @RequestBody(required = false) CrearInvitacionLinkDTO dto) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(grupoService.generarLinkInvitacion(id, usuarioId, dto));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al generar link: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/invitacion-link")
    public ResponseEntity<?> listarLinksInvitacion(@PathVariable Long id) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            return ResponseEntity.ok(grupoService.listarLinksActivos(id, usuarioId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/invitacion-link/{linkId}")
    public ResponseEntity<?> desactivarLink(@PathVariable Long linkId) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            grupoService.desactivarLink(linkId, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Link desactivado"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // INFO PÚBLICA DE INVITACIÓN (sin auth)
    // ============================================================
    @GetMapping("/invitacion/{token}")
    public ResponseEntity<InfoInvitacionDTO> infoInvitacion(@PathVariable String token) {
        return ResponseEntity.ok(grupoService.obtenerInfoInvitacion(token));
    }

    // ============================================================
    // UNIRSE CON LINK (requiere auth)
    // ============================================================
    @PostMapping("/invitacion/{token}/unirse")
    public ResponseEntity<?> unirseConLink(@PathVariable String token) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Debes iniciar sesión para unirte"));
            }
            return ResponseEntity.ok(grupoService.unirseConLink(token, usuarioId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error al unirse con link: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<?> listarHistorial(@PathVariable Long id) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return ResponseEntity.ok(grupoService.listarHistorial(id, usuarioId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}