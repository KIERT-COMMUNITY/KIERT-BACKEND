// src/main/java/com/kiert/backend/features/grupo/controller/GrupoChatController.java
package com.kiert.backend.features.grupo.controller;

import com.kiert.backend.features.grupo.dto.*;
import com.kiert.backend.features.invitacion.dto.CrearInvitacionLinkDTO;
import com.kiert.backend.features.invitacion.dto.InfoInvitacionDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.grupo.service.GrupoChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
public class GrupoChatController {

    private final GrupoChatService grupoService;
    private final UsuarioActual usuarioActual;

    private Long usuarioIdObligatorio() {
        return usuarioActual.id();
    }

    // ============================================================
    // CREAR GRUPO
    // ============================================================
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crearGrupo(@RequestBody CrearGrupoDTO dto) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(grupoService.crearGrupo(usuarioId, dto));
        } catch (Exception e) {
            log.warn("Error al crear grupo por usuario {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo crear el grupo"));
        }
    }

    // ============================================================
    // LISTAR
    // ============================================================
    @GetMapping("/mis-grupos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GrupoDTO>> misGrupos() {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(grupoService.listarMisGrupos(usuarioId));
    }

    @GetMapping("/publicos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GrupoDTO>> gruposPublicos() {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(grupoService.listarGruposPublicos(usuarioId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerGrupo(@PathVariable Long id) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(grupoService.obtenerGrupo(id, usuarioId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Grupo no encontrado"));
        }
    }

    // ============================================================
    // MIEMBROS
    // ============================================================
    @GetMapping("/{id}/miembros")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listarMiembros(@PathVariable Long id) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(grupoService.listarMiembros(id, usuarioId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudieron listar los miembros"));
        }
    }

    // ============================================================
    // MENSAJES
    // ============================================================
    @GetMapping("/{id}/mensajes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerMensajes(@PathVariable Long id) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(grupoService.obtenerMensajes(id, usuarioId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/mensajes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> enviarMensaje(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String contenido = body != null ? body.get("contenido") : null;
        if (contenido == null || contenido.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mensaje vacio"));
        }

        try {
            return ResponseEntity.ok(grupoService.enviarMensaje(id, usuarioId, contenido));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.warn("Error al enviar mensaje en grupo {} por usuario {}: {}", id, usuarioId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo enviar el mensaje"));
        }
    }

    @PostMapping("/{id}/mensajes/con-archivo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> enviarMensajeConArchivo(
            @PathVariable Long id,
            @RequestParam(value = "contenido", required = false) String contenido,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if ((contenido == null || contenido.trim().isEmpty())
                && (archivo == null || archivo.isEmpty())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mensaje o archivo requerido"));
        }

        try {
            return ResponseEntity.ok(grupoService.enviarMensajeConArchivo(id, usuarioId, contenido, archivo));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.warn("Error al enviar mensaje con archivo en grupo {} por usuario {}: {}",
                    id, usuarioId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo enviar el mensaje"));
        }
    }

    @PostMapping("/{id}/mensajes/sticker")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> enviarSticker(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String stickerUrl = body != null ? body.get("stickerUrl") : null;
        String stickerNombre = body != null ? body.get("stickerNombre") : null;

        if (stickerUrl == null || stickerUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL del sticker requerida"));
        }

        try {
            return ResponseEntity.ok(grupoService.enviarStickerGrupo(id, usuarioId, stickerUrl, stickerNombre));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.warn("Error al enviar sticker en grupo {} por usuario {}: {}", id, usuarioId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo enviar el sticker"));
        }
    }

    // ============================================================
    // INVITACIONES
    // ============================================================
    @PostMapping("/{id}/invitar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> invitarUsuarios(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> body) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Long> usuariosIds = body != null ? body.get("usuariosIds") : null;
        if (usuariosIds == null || usuariosIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Lista de usuarios requerida"));
        }

        try {
            grupoService.invitarUsuarios(id, usuarioId, usuariosIds);
            return ResponseEntity.ok(Map.of("mensaje", "Invitaciones enviadas"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudieron enviar las invitaciones"));
        }
    }

    @PostMapping("/{id}/aceptar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> aceptarInvitacion(@PathVariable Long id) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            grupoService.aceptarInvitacion(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Te uniste al grupo"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> rechazarInvitacion(@PathVariable Long id) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            grupoService.rechazarInvitacion(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Invitacion rechazada"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/unirse")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unirseAGrupo(@PathVariable Long id) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            grupoService.unirseAGrupoPublico(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Te uniste al grupo"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/salir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> salirDelGrupo(@PathVariable Long id) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            grupoService.salirDelGrupo(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Saliste del grupo"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarGrupo(@PathVariable Long id) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            grupoService.eliminarGrupo(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Grupo eliminado correctamente"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.warn("Error al eliminar grupo {} por usuario {}: {}", id, usuarioId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo eliminar el grupo"));
        }
    }

    @DeleteMapping("/{id}/miembros/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> expulsarMiembro(
            @PathVariable Long id,
            @PathVariable Long usuarioId) {
        Long adminId = usuarioIdObligatorio();
        if (adminId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            grupoService.expulsarMiembro(id, adminId, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Miembro expulsado correctamente"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.warn("Error al expulsar miembro {} del grupo {} por admin {}: {}",
                    usuarioId, id, adminId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo expulsar al miembro"));
        }
    }

    @GetMapping("/invitaciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InvitacionGrupoDTO>> invitacionesPendientes() {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(grupoService.listarInvitacionesPendientes(usuarioId));
    }

    // ============================================================
    // FOTO DEL GRUPO
    // ============================================================
    @PostMapping("/{id}/foto")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> actualizarFoto(
            @PathVariable Long id,
            @RequestParam("foto") MultipartFile foto) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(grupoService.actualizarFotoGrupo(id, usuarioId, foto));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.warn("Error al subir foto del grupo {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo subir la foto"));
        }
    }

    @DeleteMapping("/{id}/foto")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarFoto(@PathVariable Long id) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(grupoService.eliminarFotoGrupo(id, usuarioId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> actualizarInfo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            String nombre = body != null ? body.get("nombre") : null;
            String descripcion = body != null ? body.get("descripcion") : null;
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> generarLinkInvitacion(
            @PathVariable Long id,
            @RequestBody(required = false) CrearInvitacionLinkDTO dto) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(grupoService.generarLinkInvitacion(id, usuarioId, dto));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.warn("Error al generar link del grupo {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo generar el link"));
        }
    }

    @GetMapping("/{id}/invitacion-link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listarLinksInvitacion(@PathVariable Long id) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(grupoService.listarLinksActivos(id, usuarioId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/invitacion-link/{linkId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> desactivarLink(@PathVariable Long linkId) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            grupoService.desactivarLink(linkId, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Link desactivado"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // INFO PUBLICA DE INVITACION (sin auth)
    // ============================================================
    @GetMapping("/invitacion/{token}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<InfoInvitacionDTO> infoInvitacion(@PathVariable String token) {
        return ResponseEntity.ok(grupoService.obtenerInfoInvitacion(token));
    }

    @PostMapping("/invitacion/{token}/unirse")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unirseConLink(@PathVariable String token) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesion para unirte"));
        }
        try {
            return ResponseEntity.ok(grupoService.unirseConLink(token, usuarioId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.warn("Error al unirse con link: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo unir al grupo"));
        }
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listarHistorial(@PathVariable Long id) {
        Long usuarioId = usuarioIdObligatorio();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(grupoService.listarHistorial(id, usuarioId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo listar el historial"));
        }
    }
}