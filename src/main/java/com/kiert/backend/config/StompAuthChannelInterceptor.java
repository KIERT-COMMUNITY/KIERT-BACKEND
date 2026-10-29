// src/main/java/com/kiert/backend/config/StompAuthChannelInterceptor.java
package com.kiert.backend.config;

import com.kiert.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[STOMP] CONNECT sin header Authorization valido");
                throw new MessagingException("Token requerido");
            }

            String token = authHeader.substring(7);
            try {
                Long usuarioId = jwtService.extraerUsuarioId(token);
                String email = jwtService.extraerEmail(token);

                if (usuarioId == null || email == null) {
                    throw new MessagingException("Token sin usuarioId/email");
                }

                if (!jwtService.esTokenValido(token, email)) {
                    throw new MessagingException("Token invalido o expirado");
                }

                Map<String, Object> attrs = accessor.getSessionAttributes();
                if (attrs != null) {
                    attrs.put("usuarioId", String.valueOf(usuarioId));
                    attrs.put("email", email);
                }

                accessor.setUser((Principal) () -> email);

                log.debug("[STOMP] Conectado: usuarioId={} email={}", usuarioId, email);

            } catch (MessagingException e) {
                throw e;
            } catch (Exception e) {
                log.error("[STOMP] Error validando JWT: {}", e.getMessage());
                throw new MessagingException("Token invalido", e);
            }
        }

        return message;
    }
}