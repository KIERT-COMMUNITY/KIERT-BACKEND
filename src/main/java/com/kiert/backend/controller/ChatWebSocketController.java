package com.kiert.backend.controller;

import com.kiert.backend.dto.EnviarMensajeDTO;
import com.kiert.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

// Canal en tiempo real para chat.component.ts. El cliente Angular se conecta a
// /ws (SockJS+STOMP) y envía a /app/chat/{emisorId}/{receptorId}; el mensaje
// se guarda y se reenvía a /topic/chat/{receptorId} (ver ChatService).
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/{emisorId}/{receptorId}")
    public void enviarMensaje(@DestinationVariable Long emisorId,
                               @DestinationVariable Long receptorId,
                               @Payload EnviarMensajeDTO datos) {
        chatService.enviarMensaje(emisorId, receptorId, datos.contenido());
    }
}
