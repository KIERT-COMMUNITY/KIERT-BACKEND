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
                log.warn("[STOMP] CONNECT sin header Authorization válido");
                return message;
            }

            String token = authHeader.substring(7);
            try {
                // 1) Extraer datos del token
                Long usuarioId = jwtService.extraerUsuarioId(token);
                String email = jwtService.extraerEmail(token);

                if (usuarioId == null || email == null) {
                    log.warn("[STOMP] Token sin usuarioId/email");
                    return message;
                }

                // 2) Validar firma + expiración contra el email esperado
                if (!jwtService.esTokenValido(token, email)) {
                    log.warn("[STOMP] Token inválido o expirado para {}", email);
                    return message;
                }

                // 3) Guardar usuarioId en sessionAttributes
                //    → ChatGlobalController lo lee desde aquí
                Map<String, Object> attrs = accessor.getSessionAttributes();
                if (attrs != null) {
                    attrs.put("usuarioId", String.valueOf(usuarioId));
                    attrs.put("email", email);
                }

                // 4) Asignar Principal (útil para @MessageMapping con Principal)
                accessor.setUser((Principal) () -> email);

                log.debug("[STOMP] Conectado: usuarioId={} email={}", usuarioId, email);

            } catch (Exception e) {
                log.error("[STOMP] Error validando JWT: {}", e.getMessage());
                // No lanzar → deja que el frame CONNECT siga, pero sin atributos
                // Si prefieres rechazar: throw new MessagingException("Token inválido", e);
            }
        }

        return message;
    }
}