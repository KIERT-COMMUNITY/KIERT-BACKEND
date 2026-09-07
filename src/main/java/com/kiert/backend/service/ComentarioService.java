package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final RespuestaComentarioRepository respuestaRepository;
    private final PostRepository postRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReaccionRepository reaccionRepository;
    private final ReaccionRespuestaRepository reaccionRespuestaRepository;
    private final NotificationService notificationService;


    // ========== COMENTARIOS ==========

    @Transactional(readOnly = true)
    public List<ComentarioDTO> listarPorPost(Long postId) {
        log.info("📋 Listando comentarios del post: {}", postId);
        List<Comentario> comentarios = comentarioRepository.findByPostIdAndEliminadoFalseOrderByFechaCreacionAsc(postId);
        return comentarios.stream()
                .map(this::toComentarioDTO)
                .collect(Collectors.toList());
    }

    // ✅ Método para crear comentario SIN imagen
    @Transactional
    public ComentarioDTO crearComentario(Long postId, Long autorId, String contenido) {
        log.info("📝 Creando comentario en post: {}, usuario: {}", postId, autorId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Comentario comentario = Comentario.builder()
                .post(post)
                .autor(autor)
                .contenido(contenido)
                .eliminado(false)
                .build();

        comentario = comentarioRepository.save(comentario);
        log.info("✅ Comentario creado con ID: {}", comentario.getId());

        // ✅ CREAR NOTIFICACIÓN
        if (!autorId.equals(post.getAutor().getId())) {
            notificationService.crearNotificacionComentario(autorId, postId, comentario.getId());
        }

        return toComentarioDTO(comentario);
    }


    @Transactional
    public void eliminarComentario(Long comentarioId, Long usuarioId) {
        log.info("🗑️ Eliminando comentario: {}", comentarioId);

        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        if (!comentario.getAutor().getId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para eliminar este comentario");
        }

        comentario.setEliminado(true);
        comentario.setFechaEliminacion(Instant.now());
        comentarioRepository.save(comentario);
        log.info("✅ Comentario {} eliminado", comentarioId);
    }

    // ========== RESPUESTAS ==========

    @Transactional(readOnly = true)
    public List<RespuestaDTO> listarRespuestas(Long comentarioId) {
        log.info("📋 Listando respuestas del comentario: {}", comentarioId);
        List<RespuestaComentario> respuestas = respuestaRepository.findByComentarioIdAndEliminadoFalseOrderByFechaCreacionAsc(comentarioId);
        return respuestas.stream()
                .map(this::toRespuestaDTO)
                .collect(Collectors.toList());
    }

    // ✅ Método para crear respuesta SIN imagen
    @Transactional
    public RespuestaDTO crearRespuesta(Long comentarioId, Long autorId, String contenido) {
        log.info("📝 Creando respuesta al comentario: {}, usuario: {}", comentarioId, autorId);

        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        RespuestaComentario respuesta = RespuestaComentario.builder()
                .comentario(comentario)
                .autor(autor)
                .contenido(contenido)
                .eliminado(false)
                .build();

        respuesta = respuestaRepository.save(respuesta);
        log.info("✅ Respuesta creada con ID: {}", respuesta.getId());

        // ✅ CREAR NOTIFICACIÓN
        if (!autorId.equals(comentario.getAutor().getId())) {
            notificationService.crearNotificacionRespuesta(autorId, comentarioId, respuesta.getId());
        }

        return toRespuestaDTO(respuesta);
    }

    @Transactional
    public void eliminarRespuesta(Long respuestaId, Long usuarioId) {
        log.info("🗑️ Eliminando respuesta: {}", respuestaId);

        RespuestaComentario respuesta = respuestaRepository.findById(respuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Respuesta no encontrada"));

        if (!respuesta.getAutor().getId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para eliminar esta respuesta");
        }

        respuesta.setEliminado(true);
        respuesta.setFechaEliminacion(Instant.now());
        respuestaRepository.save(respuesta);
        log.info("✅ Respuesta {} eliminada", respuestaId);
    }

    // ========== REACCIONES ==========

    @Transactional
    public Map<String, Long> reaccionarComentario(Long comentarioId, Long usuarioId, String tipo) {
        log.info("❤️ Reaccionando a comentario: {}, tipo: {}, usuario: {}", comentarioId, tipo, usuarioId);

        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        Reaccion reaccion = reaccionRepository.findByUsuarioIdAndComentarioId(usuarioId, comentarioId)
                .orElse(null);

        if (reaccion != null && reaccion.getTipo().equals(tipo)) {
            reaccionRepository.delete(reaccion);
            log.info("🗑️ Reacción eliminada");
        } else if (reaccion != null) {
            reaccion.setTipo(tipo);
            reaccionRepository.save(reaccion);
            log.info("🔄 Reacción actualizada a: {}", tipo);
        } else {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
            reaccion = Reaccion.builder()
                    .usuario(usuario)
                    .comentario(comentario)
                    .tipo(tipo)
                    .build();
            reaccionRepository.save(reaccion);
            log.info("✅ Nueva reacción creada: {}", tipo);
        }

        return obtenerReaccionesComentario(comentarioId);
    }

    @Transactional
    public Map<String, Long> reaccionarRespuesta(Long respuestaId, Long usuarioId, String tipo) {
        log.info("❤️ Reaccionando a respuesta: {}, tipo: {}, usuario: {}", respuestaId, tipo, usuarioId);

        RespuestaComentario respuesta = respuestaRepository.findById(respuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Respuesta no encontrada"));

        ReaccionRespuesta reaccion = reaccionRespuestaRepository.findByUsuarioIdAndRespuestaId(usuarioId, respuestaId)
                .orElse(null);

        if (reaccion != null && reaccion.getTipo().equals(tipo)) {
            reaccionRespuestaRepository.delete(reaccion);
            log.info("🗑️ Reacción a respuesta eliminada");
        } else if (reaccion != null) {
            reaccion.setTipo(tipo);
            reaccionRespuestaRepository.save(reaccion);
            log.info("🔄 Reacción a respuesta actualizada a: {}", tipo);
        } else {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
            reaccion = ReaccionRespuesta.builder()
                    .usuario(usuario)
                    .respuesta(respuesta)
                    .tipo(tipo)
                    .build();
            reaccionRespuestaRepository.save(reaccion);
            log.info("✅ Nueva reacción a respuesta creada: {}", tipo);
        }

        return obtenerReaccionesRespuesta(respuestaId);
    }

    // ========== OBTENER REACCIONES ==========

    public Map<String, Long> obtenerReaccionesComentario(Long comentarioId) {
        Map<String, Long> reacciones = new HashMap<>();
        reacciones.put("likes", 0L);
        reacciones.put("loves", 0L);

        List<Object[]> resultados = reaccionRepository.countReaccionesByComentario(comentarioId);
        for (Object[] resultado : resultados) {
            String tipo = (String) resultado[0];
            Long count = (Long) resultado[1];
            switch (tipo) {
                case "like": reacciones.put("likes", count); break;
                case "love": reacciones.put("loves", count); break;
            }
        }
        return reacciones;
    }

    public Map<String, Long> obtenerReaccionesRespuesta(Long respuestaId) {
        Map<String, Long> reacciones = new HashMap<>();
        reacciones.put("likes", 0L);
        reacciones.put("loves", 0L);

        List<Object[]> resultados = reaccionRespuestaRepository.countReaccionesByRespuesta(respuestaId);
        for (Object[] resultado : resultados) {
            String tipo = (String) resultado[0];
            Long count = (Long) resultado[1];
            switch (tipo) {
                case "like": reacciones.put("likes", count); break;
                case "love": reacciones.put("loves", count); break;
            }
        }
        return reacciones;
    }

    // ========== DTO CONVERSIONES ==========

    private AutorResumenDTO toAutorDTO(Usuario usuario) {
        return new AutorResumenDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getFotoPerfilUrl(),
                usuario.getMarcoId()
        );
    }

    private ComentarioDTO toComentarioDTO(Comentario entity) {
        Map<String, Long> reacciones = obtenerReaccionesComentario(entity.getId());

        return new ComentarioDTO(
                entity.getId(),
                toAutorDTO(entity.getAutor()),
                entity.getContenido(),
                entity.getFechaCreacion(),
                entity.getUrlImagen(),
                new ReaccionesDTO(
                        reacciones.getOrDefault("likes", 0L),
                        reacciones.getOrDefault("loves", 0L),
                        0L, 0L, 0L, 0L
                )
        );
    }

    private RespuestaDTO toRespuestaDTO(RespuestaComentario entity) {
        Map<String, Long> reacciones = obtenerReaccionesRespuesta(entity.getId());

        return new RespuestaDTO(
                entity.getId(),
                toAutorDTO(entity.getAutor()),
                entity.getContenido(),
                entity.getFechaCreacion(),
                entity.getUrlImagen(),
                new ReaccionesDTO(
                        reacciones.getOrDefault("likes", 0L),
                        reacciones.getOrDefault("loves", 0L),
                        0L, 0L, 0L, 0L
                )
        );
    }
}