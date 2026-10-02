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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";
    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;
    private final TokenBlacklistService tokenBlacklistService;  // NUEVO

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String metodo = request.getMethod();

        if (path.startsWith("/ws")) return true;
        if ("OPTIONS".equalsIgnoreCase(metodo)) return true;

        if (path.startsWith("/api/auth/")
                || path.startsWith("/swagger")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/api/archivos/")) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String header = request.getHeader("Authorization");

        log.debug("[JWT] Procesando: {} - Header: {}",
                path, header != null ? "presente" : "ausente");

        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            log.debug("[JWT] No hay token para: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIJO_BEARER.length());
        log.debug("[JWT] Token recibido: {}...",
                token.substring(0, Math.min(token.length(), 30)));

        // VERIFICAR BLACKLIST ANTES DE CUALQUIER OTRA COSA
        try {
            if (tokenBlacklistService.estaInvalidado(token)) {
                log.warn("[JWT] Token en blacklist (logout previo): {}", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Token invalidado. Inicia sesión de nuevo.\"}");
                return;
            }
        } catch (Exception e) {
            // Fail-open: si Redis cae, permitimos el token (mejor UX)
            log.error("[JWT] Error consultando blacklist: {}", e.getMessage());
        }

        try {
            String email = jwtService.extraerEmail(token);
            log.debug("[JWT] Email extraído: {}", email);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = usuarioDetailsService.loadUserByUsername(email);

                boolean valido = jwtService.esTokenValido(token, email);

                if (valido) {
                    log.info("[JWT] Autenticación exitosa para: {}", email);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.warn("[JWT] Token inválido para: {}", email);
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (Exception ex) {
            log.warn("[JWT] Error procesando token (se ignora): {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}