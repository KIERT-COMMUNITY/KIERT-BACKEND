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
                        // SWAGGER / DOCS
                        // ============================================================
                        .requestMatchers(
                                "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**",
                                "/v3/api-docs/**", "/api-docs/**", "/webjars/**"
                        ).permitAll()

                        // ============================================================
                        // AUTH
                        // ============================================================
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/auth/login").permitAll()

                        // ============================================================
                        // PRESENCIA
                        // ============================================================
                        .requestMatchers("/api/presencia/**").hasAnyRole("USER", "CEO", "ADMIN", "MODERADOR")

                        // ============================================================
                        // GRUPOS - INVITACIONES / ARCHIVOS / WEBSOCKET
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/grupos/invitacion/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/grupos/invitacion/*/unirse").authenticated()
                        .requestMatchers("/api/archivos/**").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()

                        // ============================================================
                        // BIENVENIDA
                        // ============================================================
                        // Lectura publica (invitados incluidos)
                        .requestMatchers(HttpMethod.GET, "/api/bienvenida/**").permitAll()

                        // Gestion (solo admin)
                        .requestMatchers(HttpMethod.GET,    "/api/admin/bienvenida/**").hasAnyRole("CEO", "ADMIN", "MODERADOR")
                        .requestMatchers(HttpMethod.PUT,    "/api/admin/bienvenida/**").hasAnyRole("CEO", "ADMIN", "MODERADOR")
                        .requestMatchers(HttpMethod.POST,   "/api/admin/bienvenida/**").hasAnyRole("CEO", "ADMIN", "MODERADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/bienvenida/**").hasAnyRole("CEO", "ADMIN", "MODERADOR")

                        // ============================================================
                        // BIBLIOTECA
                        // ============================================================
                        .requestMatchers(HttpMethod.GET,    "/api/biblioteca/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/biblioteca/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/biblioteca/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH,  "/api/biblioteca/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/biblioteca/**").authenticated()

                        // ============================================================
                        // PUBLICACIONES
                        // ============================================================
                        .requestMatchers(HttpMethod.GET,    "/api/publicaciones/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH,  "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/publicaciones/**").authenticated()

                        // ============================================================
                        // COMENTARIOS
                        // ============================================================
                        .requestMatchers(HttpMethod.GET,    "/api/comentarios/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/comentarios/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/comentarios/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comentarios/**").authenticated()

                        // ============================================================
                        // REACCIONES
                        // ============================================================
                        .requestMatchers(HttpMethod.GET,  "/api/reacciones/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reacciones/**").authenticated()

                        // ============================================================
                        // ANUNCIOS (solo lectura publica, escritura denegada)
                        // ============================================================
                        .requestMatchers(HttpMethod.GET,    "/api/anuncios/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/anuncios/**").denyAll()
                        .requestMatchers(HttpMethod.PUT,    "/api/anuncios/**").denyAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/anuncios/**").denyAll()
                        .requestMatchers(HttpMethod.PATCH,  "/api/anuncios/**").denyAll()

                        // ============================================================
                        // ADMIN (bloque general, cubre /api/admin/** que no sea auth)
                        // ============================================================
                        .requestMatchers("/api/admin/**").hasAnyRole("CEO", "ADMIN", "MODERADOR")

                        // ============================================================
                        // CHAT GLOBAL (compatibilidad)
                        // ============================================================
                        .requestMatchers(HttpMethod.GET,  "/api/chat-global/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/chat-global/**").authenticated()

                        // ============================================================
                        // CHAT PRIVADO / GRUPOS
                        // ============================================================
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/grupos/**").authenticated()

                        // ============================================================
                        // USUARIOS / CONTACTOS
                        // ============================================================
                        .requestMatchers("/api/usuarios/solicitud/**").authenticated()
                        .requestMatchers("/api/usuarios/contactos").authenticated()
                        .requestMatchers("/api/usuarios/solicitudes/pendientes").authenticated()

                        // ============================================================
                        // PERSONALIZACION / NOTIFICACIONES / BLOQUEOS / REPORTES / PERFIL / STICKERS / USUARIOS
                        // ============================================================
                        .requestMatchers("/api/personalizacion/**").authenticated()
                        .requestMatchers("/api/notificaciones/**").authenticated()
                        .requestMatchers("/api/bloqueos/**").authenticated()
                        .requestMatchers("/api/reportes/**").authenticated()
                        .requestMatchers("/api/perfil/**").authenticated()
                        .requestMatchers("/api/stickers/**").authenticated()
                        .requestMatchers("/api/usuarios/**").authenticated()

                        // ============================================================
                        // TODO LO DEMAS requiere autenticacion
                        // ============================================================
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:53334",
                "http://localhost:8080",
                "http://127.0.0.1:4200",
                "https://kiert-community.vercel.app"
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