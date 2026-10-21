// src/main/java/com/kiert/backend/config/StompAuthChannelInterceptor.java
package com.kiert.backend.config;

import com.kiert.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // Validar solo en el frame CONNECT (cuando el cliente se autentica)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[WS] Conexión rechazada: falta token Authorization");
                throw new IllegalArgumentException("Token de autenticación requerido");
            }

            String token = authHeader.substring(7);

            try {
                if (!jwtService.esTokenValido(token, jwtService.extraerEmail(token))) {
                    log.warn("[WS] Token inválido o expirado");
                    throw new IllegalArgumentException("Token inválido");
                }

                String email = jwtService.extraerEmail(token);
                Long usuarioId = jwtService.extraerUsuarioId(token);
                String rol = jwtService.extraerRol(token);

                if (email == null || usuarioId == null) {
                    throw new IllegalArgumentException("Token sin datos de usuario");
                }

                // Guardar en sessionAttributes para que el WebSocketEventListener lo lea
                accessor.getSessionAttributes().put("usuarioId", usuarioId);
                accessor.getSessionAttributes().put("email", email);

                // Crear autenticación
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + (rol != null ? rol : "USER"))
                );

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                accessor.setUser(auth);

                log.info("[WS] Conexión autenticada: {} (rol={})", email, rol);

            } catch (Exception e) {
                log.warn("[WS] Error validando token: {}", e.getMessage());
                throw new IllegalArgumentException("Token inválido o expirado");
            }
        }

        return message;
    }
}