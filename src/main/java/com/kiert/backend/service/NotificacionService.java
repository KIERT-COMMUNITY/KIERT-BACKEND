// src/main/java/com/kiert/backend/service/NotificationService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.NotificacionDTO;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.kiert.backend.repository.GrupoChatRepository;

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

    // ========== CREAR NOTIFICACIONES ==========

    @Transactional
    public void crearNotificacionLike(Long usuarioOrigenId, Long postId) {
        log.info("🔔 Creando notificación de LIKE para post: {}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post no encontrado"));

        if (usuarioOrigenId.equals(post.getAutor().getId())) {
            log.info("⏭️ Usuario se dio like a sí mismo, no se crea notificación");
            return;
        }

        Usuario usuarioOrigen = usuarioRepository.findById(usuarioOrigenId)
                .orElseThrow(() -> new RuntimeException("Usuario origen no encontrado"));

        String mensaje = String.format("<strong>%s</strong> le dio like a tu publicación",
                usuarioOrigen.getNombreUsuario());

        Notificacion notificacion = Notificacion.builder()
                .usuarioDestino(post.getAutor())
                .usuarioOrigen(usuarioOrigen)
                .tipo("like")
                .mensaje(mensaje)
                .post(post)
                .url("/publicacion/" + postId)
                .leida(false)
                .fechaCreacion(Instant.now())
                .build();

        notificacionRepository.save(notificacion);
        log.info("✅ Notificación de like creada");
    }

    @Transactional
    public void crearNotificacionComentario(Long usuarioOrigenId, Long postId, Long comentarioId) {
        log.info("🔔 Creando notificación de COMENTARIO para post: {}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post no encontrado"));

        if (usuarioOrigenId.equals(post.getAutor().getId())) {
            log.info("⏭️ Usuario comentó su propio post, no se crea notificación");
            return;
        }

        Usuario usuarioOrigen = usuarioRepository.findById(usuarioOrigenId)
                .orElseThrow(() -> new RuntimeException("Usuario origen no encontrado"));

        String mensaje = String.format("<strong>%s</strong> comentó en tu publicación: \"%s\"",
                usuarioOrigen.getNombreUsuario(),
                comentarioRepository.findById(comentarioId)
                        .map(c -> c.getContenido().length() > 50 ? c.getContenido().substring(0, 50) + "..." : c.getContenido())
                        .orElse(""));

        Notificacion notificacion = Notificacion.builder()
                .usuarioDestino(post.getAutor())
                .usuarioOrigen(usuarioOrigen)
                .tipo("comentario")
                .mensaje(mensaje)
                .post(post)
                .comentario(comentarioRepository.findById(comentarioId).orElse(null))
                .url("/publicacion/" + postId)
                .leida(false)
                .fechaCreacion(Instant.now())
                .build();

        notificacionRepository.save(notificacion);
        log.info("✅ Notificación de comentario creada");
    }

    @Transactional
    public void crearNotificacionRespuesta(Long usuarioOrigenId, Long comentarioId, Long respuestaId) {
        log.info("🔔 Creando notificación de RESPUESTA para comentario: {}", comentarioId);

        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RuntimeException("Comentario no encontrado"));

        if (usuarioOrigenId.equals(comentario.getAutor().getId())) {
            log.info("⏭️ Usuario respondió a su propio comentario, no se crea notificación");
            return;
        }

        Usuario usuarioOrigen = usuarioRepository.findById(usuarioOrigenId)
                .orElseThrow(() -> new RuntimeException("Usuario origen no encontrado"));

        String mensaje = String.format("<strong>%s</strong> respondió a tu comentario",
                usuarioOrigen.getNombreUsuario());

        Notificacion notificacion = Notificacion.builder()
                .usuarioDestino(comentario.getAutor())
                .usuarioOrigen(usuarioOrigen)
                .tipo("respuesta")
                .mensaje(mensaje)
                .post(comentario.getPost())
                .comentario(comentario)
                .respuesta(respuestaRepository.findById(respuestaId).orElse(null))
                .url("/publicacion/" + comentario.getPost().getId())
                .leida(false)
                .fechaCreacion(Instant.now())
                .build();

        notificacionRepository.save(notificacion);
        log.info("✅ Notificación de respuesta creada");
    }

    @Transactional
    public void crearNotificacionSolicitud(Long usuarioOrigenId, Long usuarioDestinoId) {
        log.info("🔔 Creando notificación de SOLICITUD");

        Usuario usuarioOrigen = usuarioRepository.findById(usuarioOrigenId)
                .orElseThrow(() -> new RuntimeException("Usuario origen no encontrado"));
        Usuario usuarioDestino = usuarioRepository.findById(usuarioDestinoId)
                .orElseThrow(() -> new RuntimeException("Usuario destino no encontrado"));

        String mensaje = String.format("<strong>%s</strong> quiere ser tu contacto",
                usuarioOrigen.getNombreUsuario());

        Notificacion notificacion = Notificacion.builder()
                .usuarioDestino(usuarioDestino)
                .usuarioOrigen(usuarioOrigen)
                .tipo("solicitud")
                .mensaje(mensaje)
                .url("/chat/solicitudes")
                .leida(false)
                .fechaCreacion(Instant.now())
                .build();

        notificacionRepository.save(notificacion);
        log.info("✅ Notificación de solicitud creada");
    }

    // ========== OBTENER NOTIFICACIONES ==========

    @Transactional(readOnly = true)
    public List<NotificacionDTO> obtenerNotificaciones(Long usuarioId) {
        log.info("📋 Obteniendo notificaciones para usuario: {}", usuarioId);

        List<Notificacion> notificaciones = notificacionRepository
                .findByUsuarioDestinoIdOrderByFechaCreacionDesc(usuarioId);

        return notificaciones.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countNoLeidasByUsuario(usuarioId);
    }

    // ========== MARCAR COMO LEÍDAS ==========

    @Transactional
    public void marcarComoLeidas(List<Long> ids, Long usuarioId) {
        log.info("✅ Marcando como leídas {} notificaciones", ids.size());
        notificacionRepository.marcarComoLeidas(ids);
    }

    @Transactional
    public void marcarTodasComoLeidas(Long usuarioId) {
        log.info("✅ Marcando todas las notificaciones como leídas");
        notificacionRepository.marcarTodasComoLeidas(usuarioId);
    }

    // ========== ELIMINAR ==========

    @Transactional
    public void eliminarNotificacion(Long id, Long usuarioId) {
        log.info("🗑️ Eliminando notificación: {}", id);
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        if (!notificacion.getUsuarioDestino().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para eliminar esta notificación");
        }

        notificacionRepository.deleteById(id);
        log.info("✅ Notificación eliminada");
    }

    // ========== DTO MAPPING ==========
    private NotificacionDTO toDTO(Notificacion notificacion) {
        return new NotificacionDTO(
                notificacion.getId(),
                notificacion.getTipo(),
                notificacion.getMensaje(),
                notificacion.getLeida(),
                notificacion.getFechaCreacion(),
                notificacion.getUsuarioOrigen() != null ? notificacion.getUsuarioOrigen().getId() : null,
                notificacion.getUsuarioOrigen() != null ? notificacion.getUsuarioOrigen().getNombreUsuario() : null,
                notificacion.getUsuarioOrigen() != null ? notificacion.getUsuarioOrigen().getFotoPerfilUrl() : null,
                notificacion.getPost() != null ? notificacion.getPost().getId() : null,
                notificacion.getComentario() != null ? notificacion.getComentario().getId() : null,
                notificacion.getRespuesta() != null ? notificacion.getRespuesta().getId() : null,
                notificacion.getUrl(),
                notificacion.getGrupo() != null ? notificacion.getGrupo().getId() : null  // ✅ AGREGAR
        );
    }
    @Transactional
    public Notificacion crearNotificacionGrupo(
            Long usuarioDestinoId,
            Long usuarioOrigenId,
            String mensaje,
            Long grupoId,
            String url
    ) {
        log.info("📩 Creando notificación de grupo para usuario {}", usuarioDestinoId);

        Usuario destino = usuarioRepository.findById(usuarioDestinoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario destino no encontrado"));

        Usuario origen = usuarioOrigenId != null
                ? usuarioRepository.findById(usuarioOrigenId).orElse(null)
                : null;

        // ✅ BUSCAR Y ASIGNAR EL GRUPO
        GrupoChat grupo = grupoId != null
                ? grupoChatRepository.findById(grupoId).orElse(null)
                : null;

        Notificacion notif = Notificacion.builder()
                .usuarioDestino(destino)
                .usuarioOrigen(origen)
                .tipo("INVITACION_GRUPO")
                .mensaje(mensaje)
                .leida(false)
                .grupo(grupo)              // ✅ ESTA ES LA LÍNEA QUE FALTABA
                .url(url)
                .fechaCreacion(Instant.now())
                .build();

        Notificacion guardada = notificacionRepository.save(notif);
        log.info("✅ Notificación de grupo creada con ID: {} para grupo {}", guardada.getId(), grupoId);

        return guardada;
    }
}