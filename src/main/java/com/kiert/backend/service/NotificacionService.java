// src/main/java/com/kiert/backend/service/NotificacionService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.NotificacionDTO;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // ============================================================
    // CREAR NOTIFICACIONES
    // ============================================================

    @Transactional
    public Notificacion crearNotificacionLike(Long usuarioOrigenId, Long postId) {
        log.info("🔔 Notificación LIKE post={}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        if (usuarioOrigenId.equals(post.getAutor().getId())) {
            log.info("⏭️ Like propio, sin notificación");
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
        // ⚠️ fechaCreacion se llena con @CreationTimestamp

        return notificacionRepository.save(notif);
    }

    @Transactional
    public Notificacion crearNotificacionComentario(Long usuarioOrigenId, Long postId, Long comentarioId) {
        log.info("🔔 Notificación COMENTARIO post={}", postId);

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
    public Notificacion crearNotificacionRespuesta(Long usuarioOrigenId, Long comentarioId, Long respuestaId) {
        log.info("🔔 Notificación RESPUESTA comentario={}", comentarioId);

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
    public Notificacion crearNotificacionSolicitud(Long usuarioOrigenId, Long usuarioDestinoId) {
        log.info("🔔 Notificación SOLICITUD de {} a {}", usuarioOrigenId, usuarioDestinoId);

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
    public Notificacion crearNotificacionGrupo(
            Long usuarioDestinoId,
            Long usuarioOrigenId,
            String mensaje,
            Long grupoId,
            String url) {

        log.info("📩 Notificación INVITACION_GRUPO para {} (grupo {})", usuarioDestinoId, grupoId);

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
        // ⚠️ SIN .fechaCreacion() — lo hace @CreationTimestamp

        Notificacion guardada = notificacionRepository.save(notif);
        log.info("✅ Notificación de grupo creada ID={}", guardada.getId());
        return guardada;
    }

    // ============================================================
    // LECTURA
    // ============================================================

    @Transactional(readOnly = true)
    public List<NotificacionDTO> obtenerNotificaciones(Long usuarioId) {
        return notificacionRepository
                .findByUsuarioDestinoIdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countNoLeidasByUsuario(usuarioId);
    }

    // ============================================================
    // ACTUALIZAR
    // ============================================================

    @Transactional
    public void marcarComoLeidas(List<Long> ids, Long usuarioId) {
        if (ids == null || ids.isEmpty()) return;
        log.info("✅ Marcando {} como leídas (usuario {})", ids.size(), usuarioId);
        notificacionRepository.marcarComoLeidas(ids);
    }

    @Transactional
    public void marcarTodasComoLeidas(Long usuarioId) {
        log.info("✅ Marcando todas como leídas (usuario {})", usuarioId);
        notificacionRepository.marcarTodasComoLeidas(usuarioId);
    }

    @Transactional
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