// src/main/java/com/kiert/backend/controller/ChatController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.ChatService;
import com.kiert.backend.service.NotificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ChatController {

    private final ChatService chatService;
    private final NotificacionService notificacionService;
    private final UsuarioActual usuarioActual;

    // ========== CONVERSACIONES ==========
    @GetMapping("/conversaciones")
    public ResponseEntity<?> listarConversaciones() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return noAuth();
        return ResponseEntity.ok(chatService.listarConversaciones(usuarioId));
    }

    // ========== MENSAJES ==========
    @GetMapping("/{usuarioId}")
    public ResponseEntity<?> obtenerMensajes(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) return noAuth();
        return ResponseEntity.ok(chatService.obtenerMensajes(usuarioActualId, usuarioId));
    }

    @PostMapping("/{usuarioId}")
    public ResponseEntity<?> enviarMensaje(
            @PathVariable Long usuarioId,
            @Valid @RequestBody EnviarMensajeDTO request) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) return noAuth();
        try {
            return ResponseEntity.ok(chatService.enviarMensaje(usuarioActualId, usuarioId, request.contenido()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MensajeSimpleDTO(e.getMessage()));
        }
    }

    @PostMapping("/{usuarioId}/archivos")
    public ResponseEntity<?> enviarMensajeConArchivos(
            @PathVariable Long usuarioId,
            @RequestParam(value = "contenido", required = false, defaultValue = "") String contenido,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) return noAuth();
        try {
            return ResponseEntity.ok(chatService.enviarMensajeConArchivos(usuarioActualId, usuarioId, contenido, archivos));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MensajeSimpleDTO(e.getMessage()));
        }
    }

    @PutMapping("/mensajes/{usuarioId}/leidos")
    public ResponseEntity<?> marcarMensajesComoLeidos(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) return noAuth();
        chatService.marcarMensajesComoLeidos(usuarioActualId, usuarioId);
        return ResponseEntity.ok(new MensajeSimpleDTO("Mensajes marcados como leídos"));
    }

    @GetMapping("/no-leidos")
    public ResponseEntity<?> obtenerMensajesNoLeidos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return noAuth();
        return ResponseEntity.ok(chatService.obtenerMensajesNoLeidos(usuarioId));
    }

    // ========== SOLICITUDES ==========
    @GetMapping("/solicitudes")
    public ResponseEntity<?> listarSolicitudes() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return noAuth();
        return ResponseEntity.ok(chatService.listarSolicitudes(usuarioId));
    }

    @GetMapping("/solicitudes/enviadas")
    public ResponseEntity<?> listarSolicitudesEnviadas() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return noAuth();
        return ResponseEntity.ok(chatService.listarSolicitudesEnviadas(usuarioId));
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<?> enviarSolicitud(@Valid @RequestBody EnviarSolicitudDTO request) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return noAuth();

        try {
            Object solicitud = chatService.enviarSolicitud(usuarioId, request.usuarioId());

            try {
                notificacionService.crearNotificacionSolicitud(usuarioId, request.usuarioId());
            } catch (Exception e) {
                log.error("⚠️ Error creando notificación: {}", e.getMessage());
            }

            return ResponseEntity.ok(solicitud);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new MensajeSimpleDTO(e.getMessage()));
        }
    }

    @PutMapping("/solicitudes/{solicitudId}/aceptar")
    public ResponseEntity<?> aceptarSolicitud(@PathVariable Long solicitudId) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return noAuth();
        chatService.aceptarSolicitud(solicitudId, usuarioId);
        return ResponseEntity.ok(new MensajeSimpleDTO("Solicitud aceptada"));
    }

    @PutMapping("/solicitudes/{solicitudId}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(@PathVariable Long solicitudId) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return noAuth();
        chatService.rechazarSolicitud(solicitudId, usuarioId);
        return ResponseEntity.ok(new MensajeSimpleDTO("Solicitud rechazada"));
    }

    // ========== CONTACTOS ==========
    @GetMapping("/contactos/{usuarioId}")
    public ResponseEntity<?> sonContactos(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) return noAuth();
        return ResponseEntity.ok(chatService.sonContactos(usuarioActualId, usuarioId));
    }

    @GetMapping("/usuarios/disponibles")
    public ResponseEntity<?> listarUsuariosDisponibles() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) return noAuth();
        return ResponseEntity.ok(chatService.listarUsuariosDisponibles(usuarioId));
    }

    @DeleteMapping("/contactos/{usuarioId}")
    public ResponseEntity<?> eliminarContacto(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) return noAuth();
        chatService.eliminarContacto(usuarioActualId, usuarioId);
        return ResponseEntity.ok(new MensajeSimpleDTO("Contacto eliminado"));
    }

    // ========== HELPER ==========
    private ResponseEntity<?> noAuth() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MensajeSimpleDTO("Usuario no autenticado"));
    }
}