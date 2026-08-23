package com.kiert.backend.security;

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

@Slf4j  // ✅ AGREGAR ESTO
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";
    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String header = request.getHeader("Authorization");

        log.debug("🔍 [JWT] Procesando: {} - Header: {}", path, header != null ? "✅ presente" : "❌ ausente");

        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            log.debug("⛔ [JWT] No hay token para: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIJO_BEARER.length());
        log.debug("🔑 [JWT] Token recibido: {}...", token.substring(0, Math.min(token.length(), 30)));

        try {
            String email = jwtService.extraerEmail(token);
            log.debug("📧 [JWT] Email extraído: {}", email);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = usuarioDetailsService.loadUserByUsername(email);
                log.debug("👤 [JWT] UserDetails cargado: {}", userDetails.getUsername());

                boolean valido = jwtService.esTokenValido(token, email);
                log.debug("🔐 [JWT] Token válido: {}", valido);

                if (valido) {
                    log.info("✅ [JWT] Autenticación exitosa para: {}", email);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("✅ [JWT] SecurityContext actualizado");
                } else {
                    log.warn("⚠️ [JWT] Token inválido para: {}", email);
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (Exception ex) {
            log.error("❌ [JWT] Error procesando token: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}