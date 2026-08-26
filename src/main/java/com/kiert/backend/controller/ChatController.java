package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.ChatService;
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
    private final UsuarioActual usuarioActual;

    // ========== CONVERSACIONES ==========
    @GetMapping("/conversaciones")
    public ResponseEntity<?> listarConversaciones() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            log.error("❌ Usuario no autenticado en listarConversaciones");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        log.info("📋 Listando conversaciones para usuario: {}", usuarioId);
        return ResponseEntity.ok(chatService.listarConversaciones(usuarioId));
    }

    // ========== MENSAJES ==========
    @GetMapping("/{usuarioId}")
    public ResponseEntity<?> obtenerMensajes(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            log.error("❌ Usuario no autenticado en obtenerMensajes");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        log.info("💬 Obteniendo mensajes entre {} y {}", usuarioActualId, usuarioId);
        return ResponseEntity.ok(chatService.obtenerMensajes(usuarioActualId, usuarioId));
    }

    @PostMapping("/{usuarioId}")
    public ResponseEntity<?> enviarMensaje(
            @PathVariable Long usuarioId,
            @Valid @RequestBody EnviarMensajeDTO request) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            log.error("❌ Usuario no autenticado en enviarMensaje");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        log.info("📤 Enviando mensaje de {} a {}", usuarioActualId, usuarioId);
        return ResponseEntity.ok(chatService.enviarMensaje(usuarioActualId, usuarioId, request.contenido()));
    }

    // ✅ Enviar mensaje con archivos
    @PostMapping("/{usuarioId}/archivos")
    public ResponseEntity<?> enviarMensajeConArchivos(
            @PathVariable Long usuarioId,
            @RequestParam(value = "contenido", required = false, defaultValue = "") String contenido,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            log.error("❌ Usuario no autenticado en enviarMensajeConArchivos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        log.info("📤 Enviando mensaje de {} a {} con {} archivos", usuarioActualId, usuarioId,
                archivos != null ? archivos.size() : 0);
        return ResponseEntity.ok(chatService.enviarMensajeConArchivos(usuarioActualId, usuarioId, contenido, archivos));
    }

    // ✅ Marcar mensajes como leídos
    @PutMapping("/mensajes/{usuarioId}/leidos")
    public ResponseEntity<?> marcarMensajesComoLeidos(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            log.error("❌ Usuario no autenticado en marcarMensajesComoLeidos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        log.info("📬 Marcando mensajes como leídos entre {} y {}", usuarioActualId, usuarioId);
        chatService.marcarMensajesComoLeidos(usuarioActualId, usuarioId);
        return ResponseEntity.ok(new MensajeSimpleDTO("Mensajes marcados como leídos"));
    }

    // ✅ Obtener cantidad de mensajes no leídos total
    @GetMapping("/no-leidos")
    public ResponseEntity<?> obtenerMensajesNoLeidos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            log.error("❌ Usuario no autenticado en obtenerMensajesNoLeidos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        log.info("📬 Obteniendo mensajes no leídos para usuario: {}", usuarioId);
        return ResponseEntity.ok(chatService.obtenerMensajesNoLeidos(usuarioId));
    }

    // ========== SOLICITUDES ==========
    @GetMapping("/solicitudes")
    public ResponseEntity<?> listarSolicitudes() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            log.error("❌ Usuario no autenticado en listarSolicitudes");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        return ResponseEntity.ok(chatService.listarSolicitudes(usuarioId));
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<?> enviarSolicitud(@Valid @RequestBody EnviarSolicitudDTO request) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            log.error("❌ Usuario no autenticado en enviarSolicitud");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        log.info("📤 Enviando solicitud de {} a {}", usuarioId, request.usuarioId());
        return ResponseEntity.ok(chatService.enviarSolicitud(usuarioId, request.usuarioId()));
    }

    @PutMapping("/solicitudes/{solicitudId}/aceptar")
    public ResponseEntity<?> aceptarSolicitud(@PathVariable Long solicitudId) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            log.error("❌ Usuario no autenticado en aceptarSolicitud");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        log.info("✅ Aceptando solicitud {} por usuario {}", solicitudId, usuarioId);
        chatService.aceptarSolicitud(solicitudId, usuarioId);
        return ResponseEntity.ok(new MensajeSimpleDTO("Solicitud aceptada"));
    }

    @PutMapping("/solicitudes/{solicitudId}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(@PathVariable Long solicitudId) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            log.error("❌ Usuario no autenticado en rechazarSolicitud");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        log.info("❌ Rechazando solicitud {} por usuario {}", solicitudId, usuarioId);
        chatService.rechazarSolicitud(solicitudId, usuarioId);
        return ResponseEntity.ok(new MensajeSimpleDTO("Solicitud rechazada"));
    }

    // ========== CONTACTOS ==========
    @GetMapping("/contactos/{usuarioId}")
    public ResponseEntity<?> sonContactos(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            log.error("❌ Usuario no autenticado en sonContactos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        return ResponseEntity.ok(chatService.sonContactos(usuarioActualId, usuarioId));
    }

    // ✅ Listar usuarios disponibles
    @GetMapping("/usuarios/disponibles")
    public ResponseEntity<?> listarUsuariosDisponibles() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            log.error("❌ Usuario no autenticado en listarUsuariosDisponibles");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        return ResponseEntity.ok(chatService.listarUsuariosDisponibles(usuarioId));
    }

    // ✅ Eliminar contacto
    @DeleteMapping("/contactos/{usuarioId}")
    public ResponseEntity<?> eliminarContacto(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            log.error("❌ Usuario no autenticado en eliminarContacto");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MensajeSimpleDTO("Usuario no autenticado"));
        }
        log.info("🗑️ Eliminando contacto {} para usuario {}", usuarioId, usuarioActualId);
        chatService.eliminarContacto(usuarioActualId, usuarioId);
        return ResponseEntity.ok(new MensajeSimpleDTO("Contacto eliminado"));
    }
}