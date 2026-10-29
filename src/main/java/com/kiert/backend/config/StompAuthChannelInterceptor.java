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

        // ============================================================
        // CONNECT: validar JWT y setear Principal
        // ============================================================
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[STOMP] CONNECT sin header Authorization valido");
                throw new MessagingException("Token requerido");
            }

            String token = authHeader.substring(7);
            try {
                Long usuarioId = jwtService.extraerUsuarioId(token);
                String emailExtraido = jwtService.extraerEmail(token);

                // Fallback: si el token del admin no tiene email, usa el subject
                if (emailExtraido == null) {
                    try {
                        emailExtraido = jwtService.extraerSubject(token);
                    } catch (Exception ignored) { /* noop */ }
                }

                if (usuarioId == null) {
                    throw new MessagingException("Token sin usuarioId");
                }
                if (emailExtraido == null || emailExtraido.isBlank()) {
                    throw new MessagingException("Token sin email/subject");
                }
                if (!jwtService.esTokenValido(token, emailExtraido)) {
                    throw new MessagingException("Token invalido o expirado");
                }

                // ✅ variable final para la lambda
                final String email = emailExtraido;

                Map<String, Object> attrs = accessor.getSessionAttributes();
                if (attrs != null) {
                    attrs.put("usuarioId", String.valueOf(usuarioId));
                    attrs.put("email", email);
                }

                // Setear Principal (email como nombre)
                accessor.setUser((Principal) () -> email);

                log.debug("[STOMP] Conectado: usuarioId={} email={}", usuarioId, email);

            } catch (MessagingException e) {
                throw e;
            } catch (Exception e) {
                log.error("[STOMP] Error validando JWT: {}", e.getMessage());
                throw new MessagingException("Token invalido", e);
            }
        }

        // ============================================================
        // SEND: propagar usuarioId/email a los native headers
        // (por si el controller usa @Header en lugar de Principal)
        // ============================================================
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            Map<String, Object> attrs = accessor.getSessionAttributes();
            if (attrs != null) {
                Object uid = attrs.get("usuarioId");
                if (uid != null) {
                    accessor.setNativeHeader("usuarioId", String.valueOf(uid));
                }
                Object email = attrs.get("email");
                if (email != null) {
                    accessor.setNativeHeader("email", String.valueOf(email));
                }
            }
        }

        return message;
    }
}