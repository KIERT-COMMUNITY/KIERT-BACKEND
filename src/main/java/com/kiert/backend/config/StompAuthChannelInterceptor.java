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
import java.util.UUID;

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
        // CONNECT
        // ============================================================
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            // ✅ Si no hay token, permitimos conexion como INVITADO.
            // El invitado podra suscribirse a /topic/chat-global y recibir
            // mensajes en tiempo real, pero NO podra enviar (el controller
            // valida principal.getName() y lanzara BadRequestException).
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                String invitadoId = "invitado-" + UUID.randomUUID();
                accessor.setUser((Principal) () -> invitadoId);
                Map<String, Object> attrs = accessor.getSessionAttributes();
                if (attrs != null) {
                    attrs.put("invitado", "true");
                }
                log.debug("[STOMP] Conectado como invitado: {}", invitadoId);
                return message;
            }

            String token = authHeader.substring(7);
            try {
                Long usuarioId = jwtService.extraerUsuarioId(token);
                String emailExtraido = jwtService.extraerEmail(token);

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

                Map<String, Object> attrs = accessor.getSessionAttributes();
                if (attrs != null) {
                    attrs.put("usuarioId", String.valueOf(usuarioId));
                    attrs.put("email", emailExtraido);
                }

                // El Principal lleva el usuarioId como name (para el controller)
                final String usuarioIdFinal = String.valueOf(usuarioId);
                accessor.setUser((Principal) () -> usuarioIdFinal);

                log.debug("[STOMP] Conectado: usuarioId={} email={}", usuarioId, emailExtraido);

            } catch (MessagingException e) {
                throw e;
            } catch (Exception e) {
                log.error("[STOMP] Error validando JWT: {}", e.getMessage());
                throw new MessagingException("Token invalido", e);
            }
        }

        // ============================================================
        // SEND: propagar usuarioId/email a native headers
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