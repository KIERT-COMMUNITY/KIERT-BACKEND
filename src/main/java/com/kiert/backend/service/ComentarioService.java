package com.kiert.backend.service;

import com.kiert.backend.dto.*;
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

    // Nombres de caché centralizados
    private static final String CACHE_COMENTARIOS = "comentarios";
    private static final String CACHE_RESPUESTAS = "respuestas";
    private static final String CACHE_REACCIONES_COMENTARIO = "reaccionesComentario";
    private static final String CACHE_REACCIONES_RESPUESTA = "reaccionesRespuesta";

    // ============================================================
    // COMENTARIOS
    // ============================================================

    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_COMENTARIOS,
            key = "'post:' + #postId"
    )
    public List<ComentarioDTO> listarPorPost(Long postId) {
        log.info("[DB] Listando comentarios del post: {}", postId);
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
        log.info("Comentario creado con ID: {}", comentario.getId());

        if (!autorId.equals(post.getAutor().getId())) {
            notificationService.crearNotificacionComentario(autorId, postId, comentario.getId());
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

        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        if (!comentario.getAutor().getId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para eliminar este comentario");
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
    @Cacheable(
            value = CACHE_RESPUESTAS,
            key = "'comentario:' + #comentarioId"
    )
    public List<RespuestaDTO> listarRespuestas(Long comentarioId) {
        log.info("[DB] Listando respuestas del comentario: {}", comentarioId);
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
        log.info("Respuesta creada con ID: {}", respuesta.getId());

        if (!autorId.equals(comentario.getAutor().getId())) {
            notificationService.crearNotificacionRespuesta(autorId, comentarioId, respuesta.getId());
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

        RespuestaComentario respuesta = respuestaRepository.findById(respuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Respuesta no encontrada"));

        if (!respuesta.getAutor().getId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permiso para eliminar esta respuesta");
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

        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        Reaccion reaccion = reaccionRepository
                .findByUsuarioIdAndComentarioId(usuarioId, comentarioId)
                .orElse(null);

        if (reaccion != null && reaccion.getTipo().equals(tipo)) {
            reaccionRepository.delete(reaccion);
            log.info("Reacción eliminada");
        } else if (reaccion != null) {
            reaccion.setTipo(tipo);
            reaccionRepository.save(reaccion);
            log.info("Reacción actualizada a: {}", tipo);
        } else {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
            reaccion = Reaccion.builder()
                    .usuario(usuario)
                    .comentario(comentario)
                    .tipo(tipo)
                    .build();
            reaccionRepository.save(reaccion);
            log.info("Nueva reacción creada: {}", tipo);
        }

        // Consulta directa a MySQL (el caché ya fue invalidado arriba, no lo usamos aquí
        // porque queremos el valor actualizado inmediatamente para devolverlo al cliente)
        return calcularReaccionesComentarioDesdeDB(comentarioId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_REACCIONES_RESPUESTA, key = "'respuesta:' + #respuestaId"),
            @CacheEvict(value = CACHE_RESPUESTAS, allEntries = true)
    })
    public Map<String, Long> reaccionarRespuesta(Long respuestaId, Long usuarioId, String tipo) {
        log.info("Reaccionando a respuesta: {}, tipo: {}, usuario: {}", respuestaId, tipo, usuarioId);

        RespuestaComentario respuesta = respuestaRepository.findById(respuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Respuesta no encontrada"));

        ReaccionRespuesta reaccion = reaccionRespuestaRepository
                .findByUsuarioIdAndRespuestaId(usuarioId, respuestaId)
                .orElse(null);

        if (reaccion != null && reaccion.getTipo().equals(tipo)) {
            reaccionRespuestaRepository.delete(reaccion);
            log.info("Reacción a respuesta eliminada");
        } else if (reaccion != null) {
            reaccion.setTipo(tipo);
            reaccionRespuestaRepository.save(reaccion);
            log.info("Reacción a respuesta actualizada a: {}", tipo);
        } else {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
            reaccion = ReaccionRespuesta.builder()
                    .usuario(usuario)
                    .respuesta(respuesta)
                    .tipo(tipo)
                    .build();
            reaccionRespuestaRepository.save(reaccion);
            log.info("Nueva reacción a respuesta creada: {}", tipo);
        }

        return calcularReaccionesRespuestaDesdeDB(respuestaId);
    }

    // ============================================================
    // OBTENER REACCIONES (con caché)
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
    // HELPERS — cálculo directo desde BD (sin caché)
    // ============================================================

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

    // ============================================================
    // DTO CONVERSIONES
    // ============================================================

    private AutorResumenDTO toAutorDTO(Usuario usuario) {
        return new AutorResumenDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getFotoPerfilUrl(),
                usuario.getMarcoId()
        );
    }

    private ComentarioDTO toComentarioDTO(Comentario entity) {
        // Este método se llama DENTRO del método cacheado listarPorPost().
        // Cada llamada a obtenerReaccionesComentario() pasará por el proxy de caché,
        // así que aunque un post tenga 50 comentarios, solo se consultará MySQL
        // la primera vez por cada reacción (luego se lee de Redis).
        // Sin embargo, en el contexto actual (llamada interna), Spring AOP NO intercepta
        // las llamadas a métodos del mismo bean. Por eso este método usa
        // calcularReaccionesComentarioDesdeDB() directamente.
        //
        // Solución real: mover el toComentarioDTO a un helper SIN caché, y dejar que
        // listarPorPost() haga UNA sola query agregada de reacciones. Pero eso requiere
        // refactor del repository. Por ahora, el caché de listarPorPost() ya evita
        // que se repita este N+1 en cada carga del post.
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