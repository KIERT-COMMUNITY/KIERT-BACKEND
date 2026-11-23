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
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
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
    private final CorsProperties corsProperties;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/ws/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ============================================================
                        // 1) PREFLIGHT CORS — SIEMPRE PRIMERO Y PERMITIDO
                        // ============================================================
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ============================================================
                        // 2) SWAGGER / OPENAPI
                        // ============================================================
                        .requestMatchers(
                                "/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**",
                                "/v3/api-docs/**", "/api-docs/**", "/webjars/**"
                        ).permitAll()

                        // ============================================================
                        // 2.1) HEALTH CHECK — público para UptimeRobot
                        //      (evita que Render duerma el servicio)
                        // ============================================================
                        .requestMatchers("/api/health").permitAll()

                        // ============================================================
                        // 3) AUTH — endpoints públicos
                        // ============================================================
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/registro",
                                "/api/auth/verificar-cuenta",
                                "/api/auth/reenviar-codigo",
                                "/api/auth/recuperar",
                                "/api/auth/verificar-codigo-recuperacion",
                                "/api/auth/reset-password",
                                "/api/auth/reset-password-token",
                                "/api/auth/admin/clear-ratelimit"
                        ).permitAll()
                        .requestMatchers("/api/admin/auth/login").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()

                        // ============================================================
                        // 4) PRESENCIA
                        // ============================================================
                        .requestMatchers("/api/presencia/**")
                        .hasAnyRole("USER", "CEO", "ADMIN", "MODERADOR")

                        // ============================================================
                        // 5) GRUPOS / ARCHIVOS / WS
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/grupos/invitacion/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/grupos/invitacion/*/unirse").authenticated()
                        .requestMatchers("/api/archivos/**").authenticated()

                        // ============================================================
                        // 6) BIENVENIDA
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/bienvenida/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/bienvenida/**")
                        .hasAnyRole("CEO", "ADMIN", "MODERADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/bienvenida/**")
                        .hasAnyRole("CEO", "ADMIN", "MODERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/admin/bienvenida/**")
                        .hasAnyRole("CEO", "ADMIN", "MODERADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/bienvenida/**")
                        .hasAnyRole("CEO", "ADMIN", "MODERADOR")

                        // ============================================================
                        // 7) BIBLIOTECA
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/biblioteca/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/biblioteca/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/biblioteca/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/biblioteca/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/biblioteca/**").authenticated()

                        // ============================================================
                        // 8) PUBLICACIONES
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/publicaciones/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/publicaciones/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/publicaciones/**").authenticated()

                        // ============================================================
                        // 9) COMENTARIOS
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/comentarios/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comentarios/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/comentarios/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comentarios/**").authenticated()

                        // ============================================================
                        // 10) REACCIONES
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/reacciones/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reacciones/**").authenticated()

                        // ============================================================
                        // 11) ANUNCIOS (solo lectura pública)
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/anuncios/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/anuncios/**").denyAll()
                        .requestMatchers(HttpMethod.PUT, "/api/anuncios/**").denyAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/anuncios/**").denyAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/anuncios/**").denyAll()

                        // ============================================================
                        // 12) ADMIN (después de /api/admin/auth/login)
                        // ============================================================
                        .requestMatchers("/api/admin/**").hasAnyRole("CEO", "ADMIN", "MODERADOR")

                        // ============================================================
                        // 13) CHAT GLOBAL
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/chat-global/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/chat-global/**").authenticated()

                        // ============================================================
                        // 14) CHAT PRIVADO BLOQUEADO
                        // ============================================================
                        .requestMatchers("/api/chat/**").denyAll()

                        // ============================================================
                        // 15) CHAT GRUPAL
                        // ============================================================
                        .requestMatchers("/api/grupos/**").authenticated()

                        // ============================================================
                        // 16) USUARIOS / CONTACTOS
                        // ============================================================
                        .requestMatchers("/api/usuarios/solicitud/**").authenticated()
                        .requestMatchers("/api/usuarios/contactos").authenticated()
                        .requestMatchers("/api/usuarios/solicitudes/pendientes").authenticated()

                        // ============================================================
                        // 17) REPORTES
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/reportes").hasAnyRole("CEO", "ADMIN", "MODERADOR")
                        .requestMatchers(HttpMethod.GET, "/api/reportes/resumen").hasAnyRole("CEO", "ADMIN", "MODERADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/reportes/**").hasAnyRole("CEO", "ADMIN", "MODERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/reportes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/reportes/mis-reportes").authenticated()
                        .requestMatchers("/api/reportes/**").authenticated()

                        // ============================================================
                        // 18) STICKERS
                        // ============================================================
                        .requestMatchers(HttpMethod.GET, "/api/stickers/publicos").permitAll()
                        .requestMatchers("/api/stickers/**").authenticated()

                        // ============================================================
                        // 19) RESTO AUTENTICADO
                        // ============================================================
                        .requestMatchers("/api/personalizacion/**").authenticated()
                        .requestMatchers("/api/notificaciones/**").authenticated()
                        .requestMatchers("/api/bloqueos/**").authenticated()
                        .requestMatchers("/api/perfil/**").authenticated()
                        .requestMatchers("/api/usuarios/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Usa SOLO orígenes exactos (tu CorsProperties solo tiene allowedOrigins).
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
        );
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Set-Cookie",
                "X-Total-Count"
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