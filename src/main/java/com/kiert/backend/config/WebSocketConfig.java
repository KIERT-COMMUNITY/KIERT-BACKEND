// src/main/java/com/kiert/backend/config/WebSocketConfig.java
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
        // Heartbeats cada 10s en ambos sentidos. Sin esto, el proxy de Render
        // cierra la conexion por inactividad y los mensajes no llegan en vivo.
        config.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(this.messageBrokerTaskScheduler);

        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //Sin .withSockJS(): WebSocket nativo. SockJS no soporta heartbeats
        // STOMP y ademas cae a xhr-polling, lo que rompe el tiempo real.
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
                new SecurityContextChannelInterceptor()
        );
    }
}