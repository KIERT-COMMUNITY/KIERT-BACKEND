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

    // Mapa para contar sesiones activas por usuario (múltiples pestañas)
    private final Map<Long, AtomicInteger> sesionesActivas = new ConcurrentHashMap<>();

    /**
     * 🟢 Cuando se conecta un WebSocket, marcamos al usuario como en línea.
     */
    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Object usuarioIdObj = headerAccessor.getSessionAttributes() != null
                ? headerAccessor.getSessionAttributes().get("usuarioId")
                : null;

        if (usuarioIdObj != null) {
            Long usuarioId = Long.parseLong(usuarioIdObj.toString());
            log.info("🟢 WebSocket conectado para usuario: {}", usuarioId);

            sesionesActivas.computeIfAbsent(usuarioId, k -> new AtomicInteger(0)).incrementAndGet();
            usuarioService.marcarEnLinea(usuarioId);
        }
    }

    /**
     * 🔴 Cuando se desconecta un WebSocket, verificamos si es la última sesión.
     */
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Object usuarioIdObj = headerAccessor.getSessionAttributes() != null
                ? headerAccessor.getSessionAttributes().get("usuarioId")
                : null;

        if (usuarioIdObj != null) {
            Long usuarioId = Long.parseLong(usuarioIdObj.toString());
            log.info("🔌 WebSocket desconectado para usuario: {}", usuarioId);

            AtomicInteger contador = sesionesActivas.get(usuarioId);
            if (contador != null) {
                int restantes = contador.decrementAndGet();
                if (restantes <= 0) {
                    sesionesActivas.remove(usuarioId);
                    usuarioService.marcarDesconectado(usuarioId);
                    log.info("🔴 Usuario {} sin sesiones activas, marcado como desconectado", usuarioId);
                } else {
                    log.info("🟡 Usuario {} aún tiene {} sesión(es) activa(s)", usuarioId, restantes);
                }
            }
        }
    }
}