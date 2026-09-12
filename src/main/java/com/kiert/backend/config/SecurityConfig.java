package com.kiert.backend.config;

import com.kiert.backend.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ===== RUTAS PÚBLICAS =====

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/webjars/**"
                        ).permitAll()

                        // Auth (login, registro, recuperación)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Archivos públicos
                        .requestMatchers("/api/archivos/**").permitAll()

                        // ✅ PUBLICACIONES: GET público, POST/PUT/DELETE requieren auth
                        .requestMatchers(HttpMethod.GET, "/api/publicaciones/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/publicaciones/**").authenticated()

                        // ✅ Comentarios GET público
                        .requestMatchers(HttpMethod.GET, "/api/comentarios/**").permitAll()

                        // ✅ Reacciones GET público
                        .requestMatchers(HttpMethod.GET, "/api/reacciones/**").permitAll()

                        // ✅ WEBSOCKET - COMPLETAMENTE PÚBLICO
                        .requestMatchers(
                                "/ws",
                                "/ws/**",
                                "/ws/info",
                                "/ws/info/**",
                                "/ws/iframe.html",
                                "/ws/*/xhr",
                                "/ws/*/xhr_streaming",
                                "/ws/*/xhr_send",
                                "/ws/*/websocket",
                                "/ws/*/eventsource",
                                "/ws/*/htmlfile"
                        ).permitAll()

                        // ===== RUTAS PROTEGIDAS =====
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/usuarios/**").authenticated()
                        .requestMatchers("/api/personalizacion/**").authenticated()
                        .requestMatchers("/api/grupos/**").authenticated()
                        .requestMatchers("/api/notificaciones/**").authenticated()
                        .requestMatchers("/api/bloqueos/**").authenticated()
                        .requestMatchers("/api/reportes/**").authenticated()
                        .requestMatchers("/api/perfil/**").authenticated()

                        // Cualquier otra ruta requiere auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:4200",
                "http://localhost:53334",
                "http://localhost:8080",
                "http://127.0.0.1:4200",
                "*"
        ));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}