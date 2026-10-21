// src/main/java/com/kiert/backend/controller/ChatWebSocketController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.EnviarMensajeDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UsuarioActual usuarioActual;

    @MessageMapping("/chat/{receptorId}")
    public void enviarMensaje(Principal principal,
                              @Payload EnviarMensajeDTO datos,
                              @org.springframework.messaging.handler.annotation.DestinationVariable Long receptorId) {
        if (principal == null) {
            log.warn("[WS] Mensaje rechazado: no autenticado");
            return;
        }

        Long emisorId = usuarioActual.id();
        if (emisorId == null) {
            log.warn("[WS] Mensaje rechazado: sin usuario");
            return;
        }

        log.info("[WS] Mensaje de {} a {}: {}", emisorId, receptorId, datos.contenido());
        chatService.enviarMensaje(emisorId, receptorId, datos.contenido());
    }
}