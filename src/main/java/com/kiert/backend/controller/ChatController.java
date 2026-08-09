package com.kiert.backend.controller;

import com.kiert.backend.dto.ConversacionDTO;
import com.kiert.backend.dto.EnviarMensajeDTO;
import com.kiert.backend.dto.MensajeChatDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Espejo de chat.service.ts (base: environment.apiUrl + "/chat").
// El envío de mensajes también se replica en tiempo real por WebSocket
// (ver WebSocketConfig y ChatWebSocketController) para los usuarios conectados.
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UsuarioActual usuarioActual;

    @GetMapping("/conversaciones")
    public ResponseEntity<List<ConversacionDTO>> listarConversaciones() {
        return ResponseEntity.ok(chatService.listarConversaciones(usuarioActual.id()));
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<List<MensajeChatDTO>> listarMensajes(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(chatService.listarMensajes(usuarioActual.id(), usuarioId));
    }

    @PostMapping("/{usuarioId}")
    public ResponseEntity<MensajeChatDTO> enviarMensaje(@PathVariable Long usuarioId,
                                                          @Valid @RequestBody EnviarMensajeDTO datos) {
        MensajeChatDTO mensaje = chatService.enviarMensaje(usuarioActual.id(), usuarioId, datos.contenido());
        return ResponseEntity.status(HttpStatus.CREATED).body(mensaje);
    }
}
