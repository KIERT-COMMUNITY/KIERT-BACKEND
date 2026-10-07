package com.kiert.backend.service;

import com.kiert.backend.entity.AuditoriaUsuario;
import com.kiert.backend.entity.EventoAuditoria;
import com.kiert.backend.repository.AuditoriaUsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaUsuarioRepository repository;
    private final HttpServletRequest request;

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
            log.info("Auditoria [{}] {} - {}", evento, email, descripcion);
        } catch (Exception e) {
            log.error("Error al guardar auditoria: {}", e.getMessage(), e);
        }
    }

    private String obtenerIp() {
        try {
            String xf = request.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isEmpty()) {
                return xf.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String obtenerUserAgent() {
        try {
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }
}