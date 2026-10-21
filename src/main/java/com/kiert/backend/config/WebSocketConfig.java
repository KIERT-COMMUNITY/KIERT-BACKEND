// src/main/java/com/kiert/backend/config/WebSocketConfig.java
package com.kiert.backend.config;

import com.kiert.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    // Lista de orígenes permitidos (CORS estricto)
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:4200",
            "http://localhost:53334",
            "http://localhost:8080",
            "http://127.0.0.1:4200",
            "https://kiert.com",
            "https://www.kiert.com"
    );

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint con SockJS (CORS restringido)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(ALLOWED_ORIGINS.toArray(new String[0]))
                .withSockJS();

        // Endpoint WebSocket puro (sin SockJS)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(ALLOWED_ORIGINS.toArray(new String[0]));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Aquí se valida el JWT en el frame STOMP CONNECT
        registration.interceptors(new StompAuthChannelInterceptor(jwtService));
    }
}