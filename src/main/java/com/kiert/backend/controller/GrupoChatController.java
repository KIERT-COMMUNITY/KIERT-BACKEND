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

    /**
     * Crear grupo
     * POST /api/grupos
     */
    @PostMapping
    public ResponseEntity<?> crearGrupo(@RequestBody CrearGrupoDTO dto) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            GrupoDTO grupo = grupoService.crearGrupo(usuarioId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(grupo);
        } catch (Exception e) {
            log.error("❌ Error al crear grupo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Listar mis grupos
     * GET /api/grupos/mis-grupos
     */
    @GetMapping("/mis-grupos")
    public ResponseEntity<List<GrupoDTO>> misGrupos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(grupoService.listarMisGrupos(usuarioId));
    }

    /**
     * Listar grupos públicos disponibles
     * GET /api/grupos/publicos
     */
    @GetMapping("/publicos")
    public ResponseEntity<List<GrupoDTO>> gruposPublicos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(grupoService.listarGruposPublicos(usuarioId));
    }

    /**
     * Obtener grupo por ID
     * GET /api/grupos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<GrupoDTO> obtenerGrupo(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(grupoService.obtenerGrupo(id, usuarioId));
    }

    /**
     * Listar miembros del grupo
     * GET /api/grupos/{id}/miembros
     */
    @GetMapping("/{id}/mensajes")
    public ResponseEntity<List<MensajeGrupoDTO>> obtenerMensajes(@PathVariable Long id) {
        return ResponseEntity.ok(grupoService.obtenerMensajes(id));
    }

    /**
     * Invitar usuarios
     * POST /api/grupos/{id}/invitar
     */
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

    /**
     * Aceptar invitación
     * POST /api/grupos/{id}/aceptar
     */
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

    /**
     * Rechazar invitación
     * POST /api/grupos/{id}/rechazar
     */
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarInvitacion(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        grupoService.rechazarInvitacion(id, usuarioId);
        return ResponseEntity.ok(Map.of("mensaje", "Invitación rechazada"));
    }

    /**
     * Unirse a grupo público
     * POST /api/grupos/{id}/unirse
     */
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

    /**
     * Salir del grupo     * DELETE /api/grupos/{id}/salir
     */
    @DeleteMapping("/{id}/salir")
    public ResponseEntity<?> salirDelGrupo(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        grupoService.salirDelGrupo(id, usuarioId);
        return ResponseEntity.ok(Map.of("mensaje", "Saliste del grupo"));
    }

    /**
     * Listar invitaciones pendientes
     * GET /api/grupos/invitaciones
     */
    @GetMapping("/invitaciones")
    public ResponseEntity<List<InvitacionGrupoDTO>> invitacionesPendientes() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(grupoService.listarInvitacionesPendientes(usuarioId));
    }

    @PostMapping("/{id}/mensajes")
    public ResponseEntity<?> enviarMensaje(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            String contenido = body.get("contenido");
            MensajeGrupoDTO mensaje = grupoService.enviarMensaje(id, usuarioId, contenido);
            return ResponseEntity.ok(mensaje);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}