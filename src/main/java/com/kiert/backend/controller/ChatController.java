package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.ChatService;
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

    @GetMapping("/conversaciones")
    public ResponseEntity<?> listarConversaciones() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            log.error("❌ Usuario no autenticado en listarConversaciones");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        log.info("📋 Listando conversaciones para usuario: {}", usuarioId);
        return ResponseEntity.ok(chatService.listarConversaciones(usuarioId));
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<?> obtenerMensajes(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            log.error("❌ Usuario no autenticado en obtenerMensajes");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        log.info("💬 Obteniendo mensajes entre {} y {}", usuarioActualId, usuarioId);
        return ResponseEntity.ok(chatService.obtenerMensajes(usuarioActualId, usuarioId));
    }

    @GetMapping("/solicitudes")
    public ResponseEntity<?> listarSolicitudes() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            log.error("❌ Usuario no autenticado en listarSolicitudes");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        return ResponseEntity.ok(chatService.listarSolicitudes(usuarioId));
    }

    @GetMapping("/contactos/{usuarioId}")
    public ResponseEntity<?> sonContactos(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {

            log.error("❌ Usuario no autenticado en sonContactos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        return ResponseEntity.ok(chatService.sonContactos(usuarioActualId, usuarioId));
    }
}