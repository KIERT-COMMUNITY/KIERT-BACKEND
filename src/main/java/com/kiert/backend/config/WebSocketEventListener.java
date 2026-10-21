// src/main/java/com/kiert/backend/config/WebSocketEventListener.java
package com.kiert.backend.config;

import com.kiert.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UsuarioService usuarioService;

    private final Map<Long, AtomicInteger> sesionesActivas = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Object usuarioIdObj = headerAccessor.getSessionAttributes() != null
                ? headerAccessor.getSessionAttributes().get("usuarioId")
                : null;

        if (usuarioIdObj != null) {
            Long usuarioId = Long.parseLong(usuarioIdObj.toString());
            log.info("[WS] Conectado: {}", usuarioId);
            sesionesActivas.computeIfAbsent(usuarioId, k -> new AtomicInteger(0)).incrementAndGet();
            usuarioService.marcarEnLinea(usuarioId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Object usuarioIdObj = headerAccessor.getSessionAttributes() != null
                ? headerAccessor.getSessionAttributes().get("usuarioId")
                : null;

        if (usuarioIdObj != null) {
            Long usuarioId = Long.parseLong(usuarioIdObj.toString());
            log.info("[WS] Desconectado: {}", usuarioId);

            AtomicInteger contador = sesionesActivas.get(usuarioId);
            if (contador != null) {
                int restantes = contador.decrementAndGet();
                if (restantes <= 0) {
                    sesionesActivas.remove(usuarioId);
                    usuarioService.marcarDesconectado(usuarioId);
                    log.info("[WS] Usuario {} sin sesiones activas", usuarioId);
                }
            }
        }
    }
}