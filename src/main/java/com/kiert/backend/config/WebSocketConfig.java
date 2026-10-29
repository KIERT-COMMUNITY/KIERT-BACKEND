// src/main/java/com/kiert/backend/config/WebSocketConfig.java
package com.kiert.backend.config;

import com.kiert.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS requiere setAllowedOriginPatterns (no setAllowedOrigins) si
        // quieres permitir varios origenes con credenciales.
        // En produccion, restringimos a los dominios reales.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "https://kiert-community.vercel.app",
                        "https://www.kiert.com",
                        "https://kiert.com",
                        "http://localhost:4200"
                )
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // ORDEN CRITICO:
        // 1. StompAuthChannelInterceptor: lee el JWT del frame CONNECT y llama
        //    a accessor.setUser(...), dejando el Principal listo.
        // 2. SecurityContextChannelInterceptor: propaga ese Principal al
        //    SecurityContextHolder para que el frame SEND lo vea como autenticado.
        // Si los inviertes, el SEND llega como anonimo y es rechazado.
        registration.interceptors(
                new StompAuthChannelInterceptor(jwtService),
                new SecurityContextChannelInterceptor()
        );
    }
}