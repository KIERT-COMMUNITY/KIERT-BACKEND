// src/main/java/com/kiert/backend/config/SecurityConfig.java
package com.kiert.backend.config;

import com.kiert.backend.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
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

                        // ============================================================
                        // 1. RUTAS PÚBLICAS (SIN AUTENTICACIÓN)
                        // ============================================================
                        .requestMatchers(
                                "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**",
                                "/v3/api-docs/**", "/api-docs/**", "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/auth/login").permitAll()
                        .requestMatchers("/api/presencia/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/grupos/invitacion/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/grupos/invitacion/*/unirse").authenticated()
                        .requestMatchers("/api/archivos/**").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()

                        // ============================================================
                        // 2. RUTAS PÚBLICAS DE LECTURA (GET)
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/publicaciones/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comentarios/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reacciones/**").permitAll()

                        // ============================================================
                        // 3. ANUNCIOS (Lectura autenticada)
                        //    Va ANTES que /api/admin/** para no chocar
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/anuncios/publicos").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/anuncios/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/anuncios/**").denyAll()
                        .requestMatchers(HttpMethod.PUT, "/api/anuncios/**").denyAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/anuncios/**").denyAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/anuncios/**").denyAll()

                        // ============================================================
                        // 4. ADMIN (solo CEO, ADMIN, MODERADOR)
                        // ============================================================
                        .requestMatchers("/api/admin/**").hasAnyRole("CEO", "ADMIN", "MODERADOR")

                        // ============================================================
                        // 5. CHAT GLOBAL + CHAT PRIVADO
                        //    ✅ Ahora CUALQUIER usuario autenticado (admin o normal)
                        //       puede usar el chat. Antes estaba con hasRole("USER")
                        //       y bloqueaba a los admins.
                        // ============================================================
                        .requestMatchers("/api/chat-global/**").authenticated()
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/grupos/**").authenticated()

                        // ============================================================
                        // 6. RUTAS PROTEGIDAS DE USUARIO
                        // ============================================================
                        .requestMatchers("/api/usuarios/solicitud/**").authenticated()
                        .requestMatchers("/api/usuarios/contactos").authenticated()
                        .requestMatchers("/api/usuarios/solicitudes/pendientes").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/publicaciones/**").authenticated()

                        .requestMatchers("/api/personalizacion/**").authenticated()
                        .requestMatchers("/api/notificaciones/**").authenticated()
                        .requestMatchers("/api/bloqueos/**").authenticated()
                        .requestMatchers("/api/reportes/**").authenticated()
                        .requestMatchers("/api/perfil/**").authenticated()
                        .requestMatchers("/api/stickers/**").authenticated()
                        .requestMatchers("/api/usuarios/**").authenticated()

                        // ============================================================
                        // 7. CUALQUIER OTRA RUTA REQUIERE AUTH
                        // ============================================================
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:4200", "http://localhost:53334",
                "http://localhost:8080", "http://127.0.0.1:4200", "*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(
                "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"
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