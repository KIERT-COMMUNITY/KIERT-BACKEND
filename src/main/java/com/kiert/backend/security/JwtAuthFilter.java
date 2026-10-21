// src/main/java/com/kiert/backend/security/JwtAuthFilter.java
package com.kiert.backend.security;

import com.kiert.backend.service.auth.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";
    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String metodo = request.getMethod();

        if (path.startsWith("/ws") || "OPTIONS".equalsIgnoreCase(metodo)) {
            return true;
        }

        if (path.startsWith("/api/auth/")
                || path.startsWith("/api/admin/auth/login")
                || path.startsWith("/swagger")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/api/archivos/")
                || path.startsWith("/api/presencia/")) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIJO_BEARER.length());

        // ============================================================
        // 1. Verificar Blacklist (Redis)
        // ============================================================
        try {
            if (tokenBlacklistService.estaInvalidado(token)) {
                log.warn("[JWT] Token invalidado: {}", request.getRequestURI());
                enviarError(response, "Token invalidado. Inicia sesión de nuevo.");
                return;
            }
        } catch (Exception e) {
            log.error("[JWT] Error en blacklist: {}", e.getMessage());
        }

        // ============================================================
        // 2. Validar Token
        // ============================================================
        try {
            String email = jwtService.extraerEmail(token);

            if (email == null || email.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtService.esTokenValido(token, email)) {
                log.warn("[JWT] Token inválido para: {}", email);
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            UserDetails userDetails = usuarioDetailsService.loadUserByUsername(email);
            String rol = jwtService.extraerRol(token);

            // Construir authorities
            List<GrantedAuthority> authorities = new ArrayList<>();

            if (rol != null && !rol.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + rol));
            }

            // NO añadimos ROLE_USER automáticamente si es admin.
            // Solo añadimos las que vengan del UserDetails.
            userDetails.getAuthorities().forEach(auth -> {
                String authority = auth.getAuthority();
                if (authorities.stream().noneMatch(a -> a.getAuthority().equals(authority))) {
                    authorities.add(new SimpleGrantedAuthority(authority));
                }
            });

            if (authorities.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.info("[JWT] Autenticado: {} | roles={}", email,
                    authorities.stream().map(GrantedAuthority::getAuthority).toList());

        } catch (Exception ex) {
            log.warn("[JWT] Error: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        } finally {
            filterChain.doFilter(request, response);
        }
    }

    private void enviarError(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}