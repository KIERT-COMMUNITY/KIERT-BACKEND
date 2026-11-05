








// src/main/java/com/kiert/backend/config/WebSocketConfig.java
package com.kiert.backend.config;

import com.kiert.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final FeatureFlagService featureFlagService;

    private TaskScheduler messageBrokerTaskScheduler;

    @Autowired
    public void setMessageBrokerTaskScheduler(@Lazy TaskScheduler taskScheduler) {
        this.messageBrokerTaskScheduler = taskScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(this.messageBrokerTaskScheduler);

        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "https://kiert-community.vercel.app",
                        "https://www.kiert.com",
                        "https://kiert.com",
                        "https://kiert-frontend.adriandelaguila-2005.workers.dev",
                        "http://localhost:4200"
                );
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                new StompAuthChannelInterceptor(jwtService),
                new SecurityContextChannelInterceptor(),
                // BLOQUEO del chat privado: rechaza cualquier SEND/SUBSCRIBE a destinos de chat privado
                new ChannelInterceptor() {
                    @Override
                    public Message<?> preSend(Message<?> message, MessageChannel channel) {
                        if (featureFlagService.isChatPrivadoEnabled()) {
                            return message;
                        }
                        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                        String destination = accessor.getDestination();
                        if (destination != null && esDestinoChatPrivado(destination)) {
                            // Silenciosamente descarta el mensaje
                            return null;
                        }
                        return message;
                    }

                    private boolean esDestinoChatPrivado(String destination) {
                        return destination.startsWith("/app/chat.")
                                || destination.equals("/app/chat")
                                || destination.startsWith("/topic/chat.")
                                || destination.startsWith("/user/queue/chat");
                    }
                }
        );
    }
}









//chatActivo


// src/main/java/com/kiert/backend/config/WebSocketConfig.java

/*
package com.kiert.backend.config;

import com.kiert.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    private TaskScheduler messageBrokerTaskScheduler;

    @Autowired
    public void setMessageBrokerTaskScheduler(@Lazy TaskScheduler taskScheduler) {
        this.messageBrokerTaskScheduler = taskScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        // Heartbeats cada 10 segundos en ambos sentidos.
        // Ayuda a mantener activa la conexión WebSocket en producción.
        config.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(this.messageBrokerTaskScheduler);

        // Prefijo para los mensajes enviados desde el frontend al backend.
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // Endpoint WebSocket nativo.
        // Se permiten las conexiones desde los dominios autorizados.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "https://kiert-community.vercel.app",
                        "https://www.kiert.com",
                        "https://kiert.com",
                        "https://kiert-frontend.adriandelaguila-2005.workers.dev",
                        "http://localhost:4200"
                );
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        // Interceptor para validar la autenticación JWT.
        // Mantiene el contexto de seguridad de los mensajes recibidos.
        registration.interceptors(
                new StompAuthChannelInterceptor(jwtService),
                new SecurityContextChannelInterceptor()
        );
    }
}
*/