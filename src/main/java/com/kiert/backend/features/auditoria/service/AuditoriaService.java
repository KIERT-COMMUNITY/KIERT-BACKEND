// src/main/java/com/kiert/backend/service/AuditoriaService.java
package com.kiert.backend.features.auditoria.service;

import com.kiert.backend.entity.AuditoriaUsuario;
import com.kiert.backend.entity.EventoAuditoria;
import com.kiert.backend.features.auditoria.repository.AuditoriaUsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaUsuarioRepository repository;

    @Transactional
    public void registrar(Long usuarioId,
                          String email,
                          EventoAuditoria evento,
                          String descripcion,
                          boolean exito) {
        registrar(usuarioId, email, evento, descripcion, exito, null);
    }

    @Transactional
    public void registrar(Long usuarioId,
                          String email,
                          EventoAuditoria evento,
                          String descripcion,
                          boolean exito,
                          String datosExtra) {
        try {
            AuditoriaUsuario a = AuditoriaUsuario.builder()
                    .usuarioId(usuarioId)
                    .email(email)
                    .evento(evento.name())
                    .descripcion(descripcion)
                    .exito(exito)
                    .ip(obtenerIp())
                    .userAgent(obtenerUserAgent())
                    .datosExtra(datosExtra)
                    .build();
            repository.save(a);

            // ⚠️ No loguear datos sensibles completos en INFO
            log.info("Auditoria [{}] {} - {}", evento, enmascararEmail(email),
                    descripcion);
        } catch (Exception e) {
            log.error("Error al guardar auditoría: {}", e.getMessage(), e);
        }
    }

    /**
     * Obtiene la IP del cliente SIN inyectar HttpServletRequest en el constructor.
     * Esto permite usar el servicio en contextos sin request (ej. @Scheduled).
     */
    private String obtenerIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;

            HttpServletRequest request = attrs.getRequest();

            String xf = request.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isEmpty()) {
                return xf.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene el User-Agent sin inyectar HttpServletRequest.
     */
    private String obtenerUserAgent() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;

            return attrs.getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Enmascara el email para no exponerlo completo en logs.
     * Ejemplo: "juan@example.com" → "j***@e***.com"
     */
    private String enmascararEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int at = email.indexOf('@');
        if (at <= 0) return "***";

        String local = email.substring(0, at);
        String dominio = email.substring(at + 1);

        String localMask = local.length() <= 1
                ? "***"
                : local.charAt(0) + "***";

        String dominioMask;
        int dot = dominio.lastIndexOf('.');
        if (dot <= 0) {
            dominioMask = "***";
        } else {
            String extension = dominio.substring(dot);
            String nombreDominio = dominio.substring(0, dot);
            dominioMask = (nombreDominio.isEmpty() ? "***"
                    : nombreDominio.charAt(0) + "***") + extension;
        }

        return localMask + "@" + dominioMask;
    }
}