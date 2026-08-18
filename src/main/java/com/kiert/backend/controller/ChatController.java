package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UsuarioActual usuarioActual;

    @GetMapping("/conversaciones")
    public ResponseEntity<List<ConversacionDTO>> listarConversaciones() {
        try {
            log.info("📋 Listando conversaciones para usuario: {}", usuarioActual.id());
            return ResponseEntity.ok(chatService.listarConversaciones(usuarioActual.id()));
        } catch (Exception e) {
            log.error("❌ Error en listarConversaciones: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<MensajeChatDTO>> obtenerMensajes(@PathVariable Long usuarioId) {
        try {
            log.info("💬 Obteniendo mensajes con usuario: {}", usuarioId);
            return ResponseEntity.ok(chatService.obtenerMensajes(usuarioActual.id(), usuarioId));
        } catch (Exception e) {
            log.error("❌ Error en obtenerMensajes: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ✅ ENDPOINT CORREGIDO PARA ENVIAR MENSAJE
    @PostMapping("/{usuarioId}")
    public ResponseEntity<MensajeChatDTO> enviarMensaje(
            @PathVariable Long usuarioId,
            @RequestBody EnviarMensajeDTO datos) {

        log.info("📤 Enviando mensaje de {} a {}", usuarioActual.id(), usuarioId);
        log.info("📝 Contenido: {}", datos.contenido());

        MensajeChatDTO resultado = chatService.enviarMensaje(
                usuarioActual.id(),
                usuarioId,
                datos.contenido()
        );

        log.info("✅ Mensaje enviado con ID: {}", resultado.id());
        return ResponseEntity.ok(resultado);
    }

    // ✅ ENDPOINT CORREGIDO PARA ENVIAR ARCHIVOS
    @PostMapping(value = "/{usuarioId}/archivos", consumes = {"multipart/form-data"})
    public ResponseEntity<MensajeChatDTO> enviarMensajeConArchivos(
            @PathVariable Long usuarioId,
            @RequestParam(value = "contenido", required = false) String contenido,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos) {

        log.info("📤 Enviando mensaje con archivos de {} a {}", usuarioActual.id(), usuarioId);
        log.info("📝 Contenido: {}", contenido != null ? contenido : "(vacío)");
        log.info("📎 Archivos: {}", archivos != null ? archivos.size() : 0);

        MensajeChatDTO resultado = chatService.enviarMensajeConArchivos(
                usuarioActual.id(),
                usuarioId,
                contenido != null ? contenido : "",
                archivos
        );

        log.info("✅ Mensaje con archivos enviado con ID: {}", resultado.id());
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/solicitudes")
    public ResponseEntity<List<SolicitudContactoDTO>> listarSolicitudes() {
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

    @GetMapping("/contactos/{usuarioId}")
    public ResponseEntity<Boolean> sonContactos(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(chatService.sonContactos(usuarioActual.id(), usuarioId));
    }

    @GetMapping("/usuarios/disponibles")
    public ResponseEntity<List<UsuarioDisponibleDTO>> listarUsuariosDisponibles() {
        return ResponseEntity.ok(chatService.listarUsuariosDisponibles(usuarioActual.id()));
    }

    @DeleteMapping("/contactos/{usuarioId}")
    public ResponseEntity<Void> eliminarContacto(@PathVariable Long usuarioId) {
        chatService.eliminarContacto(usuarioActual.id(), usuarioId);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Chat funcionando");
    }
}