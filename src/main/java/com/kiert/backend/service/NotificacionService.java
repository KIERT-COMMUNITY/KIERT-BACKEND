// src/main/java/com/kiert/backend/service/NotificacionService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.NotificacionDTO;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PostRepository postRepository;
    private final ComentarioRepository comentarioRepository;
    private final RespuestaComentarioRepository respuestaRepository;
    private final GrupoChatRepository grupoChatRepository;

    private static final String CACHE_NOTIFICACIONES = "notificaciones";
    private static final String CACHE_CONTADOR_NOTIFICACIONES = "contadorNotificaciones";

    // ============================================================
    // CREAR NOTIFICACIONES EXISTENTES
    // ============================================================

    @Transactional
    @CacheEvict(value = CACHE_CONTADOR_NOTIFICACIONES, allEntries = true)
    public Notificacion crearNotificacionLike(Long usuarioOrigenId, Long postId) {
        log.info("Notificación LIKE post={}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        if (usuarioOrigenId.equals(post.getAutor().getId())) {
            log.info("Like propio, sin notificación");
            return null;
        }

        Usuario origen = usuarioRepository.findById(usuarioOrigenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String mensaje = String.format("<strong>%s</strong> le dio like a tu publicación",
                origen.getNombreUsuario());

        Notificacion notif = Notificacion.builder()
                .usuarioDestino(post.getAutor())
                .usuarioOrigen(origen)
                .tipo("like")
                .mensaje(mensaje)
                .post(post)
                .url("/publicacion/" + postId)
                .leida(false)
                .build();

        return notificacionRepository.save(notif);
    }

    @Transactional
    @CacheEvict(value = CACHE_CONTADOR_NOTIFICACIONES, allEntries = true)
    public Notificacion crearNotificacionComentario(Long usuarioOrigenId, Long postId, Long comentarioId) {
        log.info("Notificación COMENTARIO post={}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        if (usuarioOrigenId.equals(post.getAutor().getId())) return null;

        Usuario origen = usuarioRepository.findById(usuarioOrigenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Comentario comentario = comentarioRepository.findById(comentarioId).orElse(null);
        String preview = comentario != null && comentario.getContenido() != null
                ? (comentario.getContenido().length() > 50
                   ? comentario.getContenido().substring(0, 50) + "..."
                   : comentario.getContenido())
                : "";

        String mensaje = String.format("<strong>%s</strong> comentó: \"%s\"",
                origen.getNombreUsuario(), preview);

        Notificacion notif = Notificacion.builder()
                .usuarioDestino(post.getAutor())
                .usuarioOrigen(origen)
                .tipo("comentario")
                .mensaje(mensaje)
                .post(post)
                .comentario(comentario)
                .url("/publicacion/" + postId)
                .leida(false)
                .build();

        return notificacionRepository.save(notif);
    }

    @Transactional
    @CacheEvict(value = CACHE_CONTADOR_NOTIFICACIONES, allEntries = true)
    public Notificacion crearNotificacionRespuesta(Long usuarioOrigenId, Long comentarioId, Long respuestaId) {
        log.info("Notificación RESPUESTA comentario={}", comentarioId);

        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        if (usuarioOrigenId.equals(comentario.getAutor().getId())) return null;

        Usuario origen = usuarioRepository.findById(usuarioOrigenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String mensaje = String.format("<strong>%s</strong> respondió a tu comentario",
                origen.getNombreUsuario());

        Notificacion notif = Notificacion.builder()
                .usuarioDestino(comentario.getAutor())
                .usuarioOrigen(origen)
                .tipo("respuesta")
                .mensaje(mensaje)
                .post(comentario.getPost())
                .comentario(comentario)
                .respuesta(respuestaRepository.findById(respuestaId).orElse(null))
                .url("/publicacion/" + comentario.getPost().getId())
                .leida(false)
                .build();

        return notificacionRepository.save(notif);
    }

    @Transactional
    @CacheEvict(value = CACHE_CONTADOR_NOTIFICACIONES, allEntries = true)
    public Notificacion crearNotificacionSolicitud(Long usuarioOrigenId, Long usuarioDestinoId) {
        log.info("Notificación SOLICITUD de {} a {}", usuarioOrigenId, usuarioDestinoId);

        Usuario origen = usuarioRepository.findById(usuarioOrigenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario origen no encontrado"));
        Usuario destino = usuarioRepository.findById(usuarioDestinoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario destino no encontrado"));

        String mensaje = String.format("<strong>%s</strong> quiere ser tu contacto",
                origen.getNombreUsuario());

        Notificacion notif = Notificacion.builder()
                .usuarioDestino(destino)
                .usuarioOrigen(origen)
                .tipo("solicitud")
                .mensaje(mensaje)
                .url("/chat")
                .leida(false)
                .build();

        return notificacionRepository.save(notif);
    }

    @Transactional
    @CacheEvict(value = CACHE_CONTADOR_NOTIFICACIONES, allEntries = true)
    public Notificacion crearNotificacionGrupo(
            Long usuarioDestinoId,
            Long usuarioOrigenId,
            String mensaje,
            Long grupoId,
            String url) {

        log.info("Notificación INVITACION_GRUPO para {} (grupo {})", usuarioDestinoId, grupoId);

        Usuario destino = usuarioRepository.findById(usuarioDestinoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario destino no encontrado"));

        Usuario origen = usuarioOrigenId != null
                ? usuarioRepository.findById(usuarioOrigenId).orElse(null)
                : null;

        GrupoChat grupo = grupoId != null
                ? grupoChatRepository.findById(grupoId).orElse(null)
                : null;

        Notificacion notif = Notificacion.builder()
                .usuarioDestino(destino)
                .usuarioOrigen(origen)
                .tipo("INVITACION_GRUPO")
                .mensaje(mensaje)
                .leida(false)
                .grupo(grupo)
                .url(url != null ? url : "/chat")
                .build();

        Notificacion guardada = notificacionRepository.save(notif);
        log.info("Notificación de grupo creada ID={}", guardada.getId());
        return guardada;
    }

    // ============================================================
    // ✅ NUEVAS NOTIFICACIONES DE REPORTES
    // ============================================================

    /**
     * ✅ Notifica al REPORTANTE cuando su reporte fue resuelto o rechazado.
     */
    @Transactional
    @CacheEvict(value = CACHE_CONTADOR_NOTIFICACIONES, allEntries = true)
    public Notificacion notificarResultadoAlReportante(
            Usuario reportante,
            Usuario moderador,
            String estadoFinal,
            String tipoReporte,
            String motivoReporte,
            String notaModerador,
            String accionTomada
    ) {
        boolean resuelto = "RESUELTO".equals(estadoFinal);
        String tipo = resuelto ? "REPORTE_RESUELTO" : "REPORTE_RECHAZADO";

        StringBuilder mensaje = new StringBuilder();
        if (resuelto) {
            mensaje.append("✅ Tu reporte de <strong>")
                    .append(tipoReporte)
                    .append("</strong> fue <strong>resuelto</strong> por ")
                    .append("<strong>").append(moderador.getNombreUsuario()).append("</strong>.");
        } else {
            mensaje.append("❌ Tu reporte de <strong>")
                    .append(tipoReporte)
                    .append("</strong> fue <strong>rechazado</strong> por ")
                    .append("<strong>").append(moderador.getNombreUsuario()).append("</strong>.");
        }

        if (notaModerador != null && !notaModerador.trim().isEmpty()) {
            String nota = notaModerador.length() > 100
                    ? notaModerador.substring(0, 100) + "..."
                    : notaModerador;
            mensaje.append(" Nota: \"").append(nota).append("\"");
        }

        if (accionTomada != null && !accionTomada.equals("NINGUNA")) {
            mensaje.append(" Acción: <strong>").append(accionTomada).append("</strong>.");
        }

        Notificacion notif = Notificacion.builder()
                .usuarioDestino(reportante)
                .usuarioOrigen(moderador)
                .tipo(tipo)
                .mensaje(mensaje.toString())
                .leida(false)
                .url("/mis-reportes")
                .build();

        Notificacion guardada = notificacionRepository.save(notif);
        log.info("[NOTIF] Notificado al reportante {} (reporte {})",
                reportante.getId(), estadoFinal);
        return guardada;
    }

    /**
     * ✅ Notifica a TODOS los moderadores que hay un nuevo reporte pendiente.
     */
    @Transactional
    @CacheEvict(value = CACHE_CONTADOR_NOTIFICACIONES, allEntries = true)
    public void notificarNuevoReporteAModeradores(
            Usuario reportante,
            Usuario usuarioReportado,
            String tipoReporte,
            String motivoReporte,
            Long reporteId
    ) {
        // Buscar todos los usuarios con rol CEO/ADMIN/MODERADOR
        List<Usuario> moderadores = usuarioRepository.findByRolIn(
                List.of("CEO", "ADMIN", "MODERADOR")
        );

        if (moderadores.isEmpty()) {
            log.warn("[NOTIF] No hay moderadores activos para notificar");
            return;
        }

        // No notificar al propio reportante si es moderador
        String mensaje = String.format(
                "🚨 <strong>Nuevo reporte</strong> de tipo <strong>%s</strong> " +
                        "(motivo: %s) enviado por <strong>%s</strong>.",
                tipoReporte,
                motivoReporte,
                reportante.getNombreUsuario()
        );

        for (Usuario moderador : moderadores) {
            // No notificarse a sí mismo si el reportante es moderador
            if (moderador.getId().equals(reportante.getId())) continue;

            Notificacion notif = Notificacion.builder()
                    .usuarioDestino(moderador)
                    .usuarioOrigen(reportante)
                    .tipo("REPORTE_NUEVO")
                    .mensaje(mensaje)
                    .leida(false)
                    .url("/admin/reportes")
                    .build();

            notificacionRepository.save(notif);
        }

        log.info("[NOTIF] Notificado a {} moderadores sobre reporte {}",
                moderadores.size(), reporteId);
    }

    /**
     * ✅ Notifica al USUARIO REPORTADO cuando se toma acción sobre su contenido.
     * Ej: contenido eliminado, advertencia, baneo.
     */
    @Transactional
    @CacheEvict(value = CACHE_CONTADOR_NOTIFICACIONES, allEntries = true)
    public void notificarUsuarioReportado(
            Usuario usuarioReportado,
            Usuario moderador,
            String accionTomada,
            String motivoReporte,
            String notaModerador
    ) {
        if (usuarioReportado == null) return;

        String tipo;
        String mensaje;
        String icono;

        switch (accionTomada) {
            case "ADVERTENCIA" -> {
                tipo = "ADVERTENCIA";
                icono = "⚠️";
                mensaje = String.format(
                        "%s Has recibido una <strong>advertencia</strong> por " +
                                "<strong>%s</strong>. Motivo: %s",
                        icono, moderador.getNombreUsuario(), motivoReporte
                );
            }
            case "ELIMINADO" -> {
                tipo = "CONTENIDO_ELIMINADO";
                icono = "🗑️";
                mensaje = String.format(
                        "%s Tu contenido fue <strong>eliminado</strong> por " +
                                "<strong>%s</strong>. Motivo: %s",
                        icono, moderador.getNombreUsuario(), motivoReporte
                );
            }
            case "BANEADO" -> {
                tipo = "USUARIO_BANEADO";
                icono = "🚫";
                mensaje = String.format(
                        "%s Tu cuenta ha sido <strong>suspendida</strong> por " +
                                "<strong>%s</strong>. Motivo: %s",
                        icono, moderador.getNombreUsuario(), motivoReporte
                );
            }
            default -> {
                return; // No notificar si la acción es "NINGUNA"
            }
        }

        if (notaModerador != null && !notaModerador.trim().isEmpty()) {
            mensaje += " Nota: \"" + notaModerador.trim() + "\"";
        }

        Notificacion notif = Notificacion.builder()
                .usuarioDestino(usuarioReportado)
                .usuarioOrigen(moderador)
                .tipo(tipo)
                .mensaje(mensaje)
                .leida(false)
                .url("/perfil")
                .build();

        notificacionRepository.save(notif);
        log.info("[NOTIF] Notificado al usuario reportado {} (acción {})",
                usuarioReportado.getId(), accionTomada);
    }

    // ============================================================
    // LECTURA (caché)
    // ============================================================

    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_NOTIFICACIONES,
            key = "'usuario:' + #usuarioId"
    )
    public List<NotificacionDTO> obtenerNotificaciones(Long usuarioId) {
        log.info("[DB] Obteniendo notificaciones del usuario: {}", usuarioId);
        return notificacionRepository
                .findByUsuarioDestinoIdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_CONTADOR_NOTIFICACIONES,
            key = "'usuario:' + #usuarioId"
    )
    public long contarNoLeidas(Long usuarioId) {
        log.debug("[DB] Contando notificaciones no leídas del usuario: {}", usuarioId);
        return notificacionRepository.countNoLeidasByUsuario(usuarioId);
    }

    // ============================================================
    // ACTUALIZAR (invalidación)
    // ============================================================

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_NOTIFICACIONES, key = "'usuario:' + #usuarioId"),
            @CacheEvict(value = CACHE_CONTADOR_NOTIFICACIONES, key = "'usuario:' + #usuarioId")
    })
    public void marcarComoLeidas(List<Long> ids, Long usuarioId) {
        if (ids == null || ids.isEmpty()) return;
        log.info("Marcando {} como leídas (usuario {})", ids.size(), usuarioId);
        notificacionRepository.marcarComoLeidas(ids, usuarioId, Instant.now());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_NOTIFICACIONES, key = "'usuario:' + #usuarioId"),
            @CacheEvict(value = CACHE_CONTADOR_NOTIFICACIONES, key = "'usuario:' + #usuarioId")
    })
    public void marcarTodasComoLeidas(Long usuarioId) {
        log.info("Marcando todas como leídas (usuario {})", usuarioId);
        notificacionRepository.marcarTodasComoLeidas(usuarioId, Instant.now());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_NOTIFICACIONES, key = "'usuario:' + #usuarioId"),
            @CacheEvict(value = CACHE_CONTADOR_NOTIFICACIONES, key = "'usuario:' + #usuarioId")
    })
    public void eliminarNotificacion(Long id, Long usuarioId) {
        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación no encontrada"));

        if (!n.getUsuarioDestino().getId().equals(usuarioId)) {
            throw new SecurityException("No puedes eliminar esta notificación");
        }

        notificacionRepository.deleteById(id);
    }

    // ============================================================
    // DTO
    // ============================================================

    private NotificacionDTO toDTO(Notificacion n) {
        return new NotificacionDTO(
                n.getId(),
                n.getTipo(),
                n.getMensaje(),
                n.getLeida(),
                n.getFechaCreacion(),
                n.getUsuarioOrigen() != null ? n.getUsuarioOrigen().getId() : null,
                n.getUsuarioOrigen() != null ? n.getUsuarioOrigen().getNombreUsuario() : null,
                n.getUsuarioOrigen() != null ? n.getUsuarioOrigen().getFotoPerfilUrl() : null,
                n.getPost() != null ? n.getPost().getId() : null,
                n.getComentario() != null ? n.getComentario().getId() : null,
                n.getRespuesta() != null ? n.getRespuesta().getId() : null,
                n.getUrl(),
                n.getGrupo() != null ? n.getGrupo().getId() : null
        );
    }
}