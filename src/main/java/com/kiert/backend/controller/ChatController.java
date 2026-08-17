package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UsuarioActual usuarioActual;

    // ========== CONVERSACIONES ==========
    @GetMapping("/conversaciones")
    public ResponseEntity<List<ConversacionDTO>> listarConversaciones() {
        log.info("📋 Listando conversaciones del usuario: {}", usuarioActual.id());
        return ResponseEntity.ok(chatService.listarConversaciones(usuarioActual.id()));
    }

    // ========== MENSAJES ==========
    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<MensajeChatDTO>> obtenerMensajes(@PathVariable Long usuarioId) {
        log.info("💬 Obteniendo mensajes con usuario: {}", usuarioId);
        return ResponseEntity.ok(chatService.obtenerMensajes(usuarioActual.id(), usuarioId));
    }

    @PostMapping("/{usuarioId}")
    public ResponseEntity<MensajeChatDTO> enviarMensaje(
            @PathVariable Long usuarioId,
            @RequestBody EnviarMensajeDTO datos) {
        log.info("📤 Enviando mensaje a usuario: {}", usuarioId);
        return ResponseEntity.ok(chatService.enviarMensaje(usuarioActual.id(), usuarioId, datos.contenido()));
    }

    // ========== SOLICITUDES ==========
    @GetMapping("/solicitudes")
    public ResponseEntity<List<SolicitudContactoDTO>> listarSolicitudes() {
        log.info("📋 Listando solicitudes del usuario: {}", usuarioActual.id());
        return ResponseEntity.ok(chatService.listarSolicitudes(usuarioActual.id()));
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<SolicitudContactoDTO> enviarSolicitud(@RequestBody EnviarSolicitudDTO datos) {
        log.info("📤 Enviando solicitud a usuario: {}", datos.usuarioId());
        return ResponseEntity.ok(chatService.enviarSolicitud(usuarioActual.id(), datos.usuarioId()));
    }

    @PutMapping("/solicitudes/{solicitudId}/aceptar")
    public ResponseEntity<Void> aceptarSolicitud(@PathVariable Long solicitudId) {
        log.info("✅ Aceptando solicitud: {}", solicitudId);
        chatService.aceptarSolicitud(solicitudId, usuarioActual.id());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/solicitudes/{solicitudId}/rechazar")
    public ResponseEntity<Void> rechazarSolicitud(@PathVariable Long solicitudId) {
        log.info("❌ Rechazando solicitud: {}", solicitudId);
        chatService.rechazarSolicitud(solicitudId, usuarioActual.id());
        return ResponseEntity.ok().build();
    }

    // ========== VERIFICAR CONTACTO ==========
    @GetMapping("/contactos/{usuarioId}")
    public ResponseEntity<Boolean> sonContactos(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(chatService.sonContactos(usuarioActual.id(), usuarioId));
    }

    // ========== USUARIOS DISPONIBLES ==========
    @GetMapping("/usuarios/disponibles")
    public ResponseEntity<List<UsuarioDisponibleDTO>> listarUsuariosDisponibles() {
        log.info("📋 Listando usuarios disponibles");
        return ResponseEntity.ok(chatService.listarUsuariosDisponibles(usuarioActual.id()));
    }

    @DeleteMapping("/contactos/{usuarioId}")
    public ResponseEntity<Void> eliminarContacto(@PathVariable Long usuarioId) {
        log.info("🗑️ Eliminando contacto: {}", usuarioId);
        chatService.eliminarContacto(usuarioActual.id(), usuarioId);
        return ResponseEntity.ok().build();
    }
}