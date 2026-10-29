// src/main/java/com/kiert/backend/features/auditoria/service/AuditoriaService.java
package com.kiert.backend.features.auditoria.service;

import com.kiert.backend.entity.AuditoriaUsuario;
import com.kiert.backend.entity.EventoAuditoria;
import com.kiert.backend.features.auditoria.repository.AuditoriaUsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final int DESCRIPCION_MAX_LENGTH = 500;

    private final AuditoriaUsuarioRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long usuarioId,
                          String email,
                          EventoAuditoria evento,
                          String descripcion,
                          boolean exito) {
        registrar(usuarioId, email, evento, descripcion, exito, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
                    .descripcion(truncar(descripcion, DESCRIPCION_MAX_LENGTH))
                    .exito(exito)
                    .ip(obtenerIp())
                    .userAgent(truncar(obtenerUserAgent(), 512))
                    .datosExtra(datosExtra)
                    .build();
            repository.save(a);

            log.info("Auditoria [{}] {} - {}", evento, enmascararEmail(email),
                    truncar(descripcion, 200));
        } catch (Exception e) {
            // No relanzar: la auditoria nunca debe romper el flujo principal
            log.warn("Error al guardar auditoria [{}]: {}", evento, e.getMessage());
        }
    }

    /**
     * Obtiene la IP del cliente.
     *
     * SEGURIDAD: si server.forward-headers-strategy=framework esta configurado,
     * Spring resuelve la IP real a traves de los proxies de confianza.
     * No confiamos en X-Forwarded-For directamente porque es falsificable.
     */
    private String obtenerIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;

            HttpServletRequest request = attrs.getRequest();
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

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

    private String truncar(String valor, int max) {
        if (valor == null) return null;
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}