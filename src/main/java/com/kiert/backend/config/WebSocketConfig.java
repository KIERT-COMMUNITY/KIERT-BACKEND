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

    // TaskScheduler para los heartbeats del SimpleBroker.
    // Se inyecta con @Lazy para romper el ciclo de dependencias entre
    // WebSocketConfig y la infraestructura de mensajería de Spring.
    private TaskScheduler messageBrokerTaskScheduler;

    @Autowired
    public void setMessageBrokerTaskScheduler(@Lazy TaskScheduler taskScheduler) {
        this.messageBrokerTaskScheduler = taskScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Heartbeats: el servidor envia un latido cada 10s y espera uno del
        // cliente cada 10s. Esto mantiene viva la conexion a traves de proxies
        // y balanceadores que cierran conexiones inactivas (~60s por defecto).
        config.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(this.messageBrokerTaskScheduler);

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
                        "https://kiert-frontend.adriandelaguila-2005.workers.dev", // <-- AGREGAR
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