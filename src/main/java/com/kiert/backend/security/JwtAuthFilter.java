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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";
    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    /**
     * ✅ NO procesar estas rutas con JWT.
     * Importante: solo rutas TOTALMENTE públicas.
     * Las rutas que "a veces" requieren auth NO deben saltarse.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String metodo = request.getMethod();

        // 1. WebSocket
        if (path.startsWith("/ws")) {
            return true;
        }

        // 2. OPTIONS preflight (CORS)
        if ("OPTIONS".equalsIgnoreCase(metodo)) {
            return true;
        }

        // 3. Rutas 100% públicas (nunca requieren auth)
        if (path.startsWith("/api/auth/")
                || path.startsWith("/swagger")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/api/archivos/")) {
            return true;
        }

        // ⚠️ IMPORTANTE: /api/publicaciones NO se salta el filtro.
        //    Aunque sea un endpoint con partes públicas,
        //    el filtro debe procesar el token si viene.
        //    Si no viene token → sigue como anónimo, y Spring Security decide.

        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String header = request.getHeader("Authorization");

        log.debug("🔍 [JWT] Procesando: {} - Header: {}",
                path, header != null ? "✅ presente" : "❌ ausente");

        // Si no hay token, seguir sin autenticar.
        // Spring Security decidirá si la ruta requiere auth.
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            log.debug("⛔ [JWT] No hay token para: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIJO_BEARER.length());
        log.debug("🔑 [JWT] Token recibido: {}...",
                token.substring(0, Math.min(token.length(), 30)));

        try {
            String email = jwtService.extraerEmail(token);
            log.debug("📧 [JWT] Email extraído: {}", email);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = usuarioDetailsService.loadUserByUsername(email);

                boolean valido = jwtService.esTokenValido(token, email);

                if (valido) {
                    log.info("✅ [JWT] Autenticación exitosa para: {}", email);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.warn("⚠️ [JWT] Token inválido para: {}", email);
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (Exception ex) {
            // ✅ NO relanzar. Solo loguear. Spring Security devolverá 401 al no haber auth.
            log.warn("❌ [JWT] Error procesando token (se ignora): {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}