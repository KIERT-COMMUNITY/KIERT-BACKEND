// src/main/java/com/kiert/backend/features/comentarios/service/ComentarioService.java
package com.kiert.backend.features.comentarios.service;

import com.kiert.backend.entity.*;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.autor.dto.AutorResumenDTO;
import com.kiert.backend.features.comentarios.dto.ComentarioDTO;
import com.kiert.backend.features.comentarios.repository.ComentarioRepository;
import com.kiert.backend.features.comentarios.repository.RespuestaComentarioRepository;
import com.kiert.backend.features.post.repository.PostRepository;
import com.kiert.backend.features.reacciones.dto.ReaccionesDTO;
import com.kiert.backend.features.reacciones.repository.ReaccionRepository;
import com.kiert.backend.features.reacciones.repository.ReaccionRespuestaRepository;
import com.kiert.backend.features.respuesta.dto.RespuestaDTO;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import com.kiert.backend.features.notificaciones.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    private final NotificacionService notificationService;

    private static final String CACHE_COMENTARIOS = "comentarios";
    private static final String CACHE_RESPUESTAS = "respuestas";
    private static final String CACHE_REACCIONES_COMENTARIO = "reaccionesComentario";
    private static final String CACHE_REACCIONES_RESPUESTA = "reaccionesRespuesta";

    private static final int MAX_CONTENIDO = 2000;
    private static final List<String> TIPOS_REACCION_VALIDOS =
            List.of("like", "love", "haha", "wow", "sad", "angry");

    // ============================================================
    // COMENTARIOS
    // ============================================================

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_COMENTARIOS, key = "'post:' + #postId")
    public List<ComentarioDTO> listarPorPost(Long postId) {
        log.debug("[DB] Listando comentarios del post: {}", postId);
        List<Comentario> comentarios = comentarioRepository
                .findByPostIdAndEliminadoFalseOrderByFechaCreacionAsc(postId);
        return comentarios.stream()
                .map(this::toComentarioDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_COMENTARIOS, key = "'post:' + #postId"),
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", key = "#postId")
    })
    public ComentarioDTO crearComentario(Long postId, Long autorId, String contenido) {
        log.info("Creando comentario en post: {}, usuario: {}", postId, autorId);

        validarContenido(contenido, "El comentario");

        if (postId == null || autorId == null) {
            throw new BadRequestException("Parametros invalidos");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        if (post.isEliminado()) {
            throw new BadRequestException("El post ya no esta disponible");
        }

        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Comentario comentario = Comentario.builder()
                .post(post)
                .autor(autor)
                .contenido(contenido.trim())
                .eliminado(false)
                .build();

        comentario = comentarioRepository.save(comentario);
        log.info("Comentario creado con ID: {}", comentario.getId());

        if (!autorId.equals(post.getAutor().getId())) {
            try {
                notificationService.crearNotificacionComentario(autorId, postId, comentario.getId());
            } catch (Exception e) {
                log.warn("Error creando notificacion: {}", e.getMessage());
            }
        }

        return toComentarioDTO(comentario);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_COMENTARIOS, allEntries = true),
            @CacheEvict(value = CACHE_RESPUESTAS, allEntries = true),
            @CacheEvict(value = CACHE_REACCIONES_COMENTARIO, allEntries = true),
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", allEntries = true)
    })
    public void eliminarComentario(Long comentarioId, Long usuarioId) {
        log.info("Eliminando comentario: {}", comentarioId);

        if (comentarioId == null || usuarioId == null) {
            throw new BadRequestException("Parametros invalidos");
        }

        Comentario comentario = comentarioRepository.findActivoConAutor(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        if (!comentario.getAutor().getId().equals(usuarioId)) {
            throw new BadRequestException("No tienes permiso para eliminar este comentario");
        }

        comentario.setEliminado(true);
        comentario.setFechaEliminacion(Instant.now());
        comentarioRepository.save(comentario);
        log.info("Comentario {} eliminado", comentarioId);
    }

    // ============================================================
    // RESPUESTAS
    // ============================================================

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_RESPUESTAS, key = "'comentario:' + #comentarioId")
    public List<RespuestaDTO> listarRespuestas(Long comentarioId) {
        log.debug("[DB] Listando respuestas del comentario: {}", comentarioId);
        List<RespuestaComentario> respuestas = respuestaRepository
                .findByComentarioIdAndEliminadoFalseOrderByFechaCreacionAsc(comentarioId);
        return respuestas.stream()
                .map(this::toRespuestaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_RESPUESTAS, key = "'comentario:' + #comentarioId"),
            @CacheEvict(value = CACHE_COMENTARIOS, allEntries = true)
    })
    public RespuestaDTO crearRespuesta(Long comentarioId, Long autorId, String contenido) {
        log.info("Creando respuesta al comentario: {}, usuario: {}", comentarioId, autorId);

        validarContenido(contenido, "La respuesta");

        if (comentarioId == null || autorId == null) {
            throw new BadRequestException("Parametros invalidos");
        }

        Comentario comentario = comentarioRepository.findActivoConAutor(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        if (comentario.isEliminado()) {
            throw new BadRequestException("El comentario ya no esta disponible");
        }

        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        RespuestaComentario respuesta = RespuestaComentario.builder()
                .comentario(comentario)
                .autor(autor)
                .contenido(contenido.trim())
                .eliminado(false)
                .build();

        respuesta = respuestaRepository.save(respuesta);
        log.info("Respuesta creada con ID: {}", respuesta.getId());

        if (!autorId.equals(comentario.getAutor().getId())) {
            try {
                notificationService.crearNotificacionRespuesta(autorId, comentarioId, respuesta.getId());
            } catch (Exception e) {
                log.warn("Error creando notificacion: {}", e.getMessage());
            }
        }

        return toRespuestaDTO(respuesta);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_RESPUESTAS, allEntries = true),
            @CacheEvict(value = CACHE_REACCIONES_RESPUESTA, allEntries = true)
    })
    public void eliminarRespuesta(Long respuestaId, Long usuarioId) {
        log.info("Eliminando respuesta: {}", respuestaId);

        if (respuestaId == null || usuarioId == null) {
            throw new BadRequestException("Parametros invalidos");
        }

        RespuestaComentario respuesta = respuestaRepository.findActivaById(respuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Respuesta no encontrada"));

        if (!respuesta.getAutor().getId().equals(usuarioId)) {
            throw new BadRequestException("No tienes permiso para eliminar esta respuesta");
        }

        respuesta.setEliminado(true);
        respuesta.setFechaEliminacion(Instant.now());
        respuestaRepository.save(respuesta);
        log.info("Respuesta {} eliminada", respuestaId);
    }

    // ============================================================
    // REACCIONES
    // ============================================================

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_REACCIONES_COMENTARIO, key = "'comentario:' + #comentarioId"),
            @CacheEvict(value = CACHE_COMENTARIOS, allEntries = true)
    })
    public Map<String, Long> reaccionarComentario(Long comentarioId, Long usuarioId, String tipo) {
        log.info("Reaccionando a comentario: {}, tipo: {}, usuario: {}", comentarioId, tipo, usuarioId);

        if (comentarioId == null || usuarioId == null) {
            throw new BadRequestException("Parametros invalidos");
        }
        if (tipo == null || !TIPOS_REACCION_VALIDOS.contains(tipo)) {
            throw new BadRequestException("Tipo de reaccion invalido");
        }

        Comentario comentario = comentarioRepository.findActivoConAutor(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        if (comentario.isEliminado()) {
            throw new BadRequestException("El comentario ya no esta disponible");
        }

        Reaccion reaccion = reaccionRepository
                .findByUsuarioIdAndComentarioId(usuarioId, comentarioId)
                .orElse(null);

        if (reaccion != null && reaccion.getTipo().equals(tipo)) {
            reaccionRepository.delete(reaccion);
            log.info("Reaccion eliminada");
        } else if (reaccion != null) {
            reaccion.setTipo(tipo);
            reaccionRepository.save(reaccion);
            log.info("Reaccion actualizada a: {}", tipo);
        } else {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
            reaccion = Reaccion.builder()
                    .usuario(usuario)
                    .comentario(comentario)
                    .tipo(tipo)
                    .build();
            reaccionRepository.save(reaccion);
            log.info("Nueva reaccion creada: {}", tipo);
        }

        return calcularReaccionesComentarioDesdeDB(comentarioId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_REACCIONES_RESPUESTA, key = "'respuesta:' + #respuestaId"),
            @CacheEvict(value = CACHE_RESPUESTAS, allEntries = true)
    })
    public Map<String, Long> reaccionarRespuesta(Long respuestaId, Long usuarioId, String tipo) {
        log.info("Reaccionando a respuesta: {}, tipo: {}, usuario: {}", respuestaId, tipo, usuarioId);

        if (respuestaId == null || usuarioId == null) {
            throw new BadRequestException("Parametros invalidos");
        }
        if (tipo == null || !TIPOS_REACCION_VALIDOS.contains(tipo)) {
            throw new BadRequestException("Tipo de reaccion invalido");
        }

        RespuestaComentario respuesta = respuestaRepository.findActivaById(respuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Respuesta no encontrada"));

        if (respuesta.isEliminado()) {
            throw new BadRequestException("La respuesta ya no esta disponible");
        }

        ReaccionRespuesta reaccion = reaccionRespuestaRepository
                .findByUsuarioIdAndRespuestaId(usuarioId, respuestaId)
                .orElse(null);

        if (reaccion != null && reaccion.getTipo().equals(tipo)) {
            reaccionRespuestaRepository.delete(reaccion);
            log.info("Reaccion a respuesta eliminada");
        } else if (reaccion != null) {
            reaccion.setTipo(tipo);
            reaccionRespuestaRepository.save(reaccion);
            log.info("Reaccion a respuesta actualizada a: {}", tipo);
        } else {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
            reaccion = ReaccionRespuesta.builder()
                    .usuario(usuario)
                    .respuesta(respuesta)
                    .tipo(tipo)
                    .build();
            reaccionRespuestaRepository.save(reaccion);
            log.info("Nueva reaccion a respuesta creada: {}", tipo);
        }

        return calcularReaccionesRespuestaDesdeDB(respuestaId);
    }

    // ============================================================
    // OBTENER REACCIONES (con cache)
    // ============================================================

    @Cacheable(
            value = CACHE_REACCIONES_COMENTARIO,
            key = "'comentario:' + #comentarioId"
    )
    public Map<String, Long> obtenerReaccionesComentario(Long comentarioId) {
        log.debug("[DB] Calculando reacciones del comentario: {}", comentarioId);
        return calcularReaccionesComentarioDesdeDB(comentarioId);
    }

    @Cacheable(
            value = CACHE_REACCIONES_RESPUESTA,
            key = "'respuesta:' + #respuestaId"
    )
    public Map<String, Long> obtenerReaccionesRespuesta(Long respuestaId) {
        log.debug("[DB] Calculando reacciones de la respuesta: {}", respuestaId);
        return calcularReaccionesRespuestaDesdeDB(respuestaId);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private void validarContenido(String contenido, String campo) {
        if (contenido == null || contenido.isBlank()) {
            throw new BadRequestException(campo + " no puede estar vacio");
        }
        if (contenido.length() > MAX_CONTENIDO) {
            throw new BadRequestException(campo + " es demasiado largo (max. "
                    + MAX_CONTENIDO + " caracteres)");
        }
    }

    private Map<String, Long> calcularReaccionesComentarioDesdeDB(Long comentarioId) {
        Map<String, Long> reacciones = new HashMap<>();
        reacciones.put("likes", 0L);
        reacciones.put("loves", 0L);

        List<Object[]> resultados = reaccionRepository.countReaccionesByComentario(comentarioId);
        for (Object[] resultado : resultados) {
            String tipo = (String) resultado[0];
            Long count = (Long) resultado[1];
            switch (tipo) {
                case "like" -> reacciones.put("likes", count);
                case "love" -> reacciones.put("loves", count);
            }
        }
        return reacciones;
    }

    private Map<String, Long> calcularReaccionesRespuestaDesdeDB(Long respuestaId) {
        Map<String, Long> reacciones = new HashMap<>();
        reacciones.put("likes", 0L);
        reacciones.put("loves", 0L);

        List<Object[]> resultados = reaccionRespuestaRepository.countReaccionesByRespuesta(respuestaId);
        for (Object[] resultado : resultados) {
            String tipo = (String) resultado[0];
            Long count = (Long) resultado[1];
            switch (tipo) {
                case "like" -> reacciones.put("likes", count);
                case "love" -> reacciones.put("loves", count);
            }
        }
        return reacciones;
    }

    private AutorResumenDTO toAutorDTO(Usuario usuario) {
        return new AutorResumenDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getFotoPerfilUrl(),
                usuario.getMarcoId()
        );
    }

    private ComentarioDTO toComentarioDTO(Comentario entity) {
        Map<String, Long> reacciones = calcularReaccionesComentarioDesdeDB(entity.getId());

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
        Map<String, Long> reacciones = calcularReaccionesRespuestaDesdeDB(entity.getId());

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