// src/main/java/com/kiert/backend/service/ReaccionService.java
package com.kiert.backend.service;

import com.kiert.backend.entity.Reaccion;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.entity.Post;
import com.kiert.backend.entity.Comentario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.ReaccionRepository;
import com.kiert.backend.repository.UsuarioRepository;
import com.kiert.backend.repository.PostRepository;
import com.kiert.backend.repository.ComentarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReaccionService {

    private final ReaccionRepository reaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PostRepository postRepository;
    private final ComentarioRepository comentarioRepository;
    private final NotificacionService notificationService;

    // Nombres de caché centralizados
    private static final String CACHE_REACCIONES_POST = "reaccionesPost";
    private static final String CACHE_REACCIONES_COMENTARIO = "reaccionesComentario";
    private static final String CACHE_USUARIO_REACCIONO_POST = "usuarioReaccionoPost";
    private static final String CACHE_USUARIO_REACCIONO_COMENTARIO = "usuarioReaccionoComentario";

    // ============================================================
    // REACCIONES A POSTS (escritura)
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_REACCIONES_POST, key = "#postId"),
            // Invalidar el flag "usuario reaccionó" para este par (usuario, post)
            @CacheEvict(value = CACHE_USUARIO_REACCIONO_POST,
                    key = "#usuarioId + ':' + #postId"),
            // Invalidar cachés de posts (contadores de reacciones)
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", key = "#postId")
    })
    public Map<String, Long> reaccionarPost(Long usuarioId, Long postId, String tipo) {
        log.info("Usuario {} reaccionando a post {} con tipo {}", usuarioId, postId, tipo);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        Optional<Reaccion> reaccionExistente = reaccionRepository.findByUsuarioIdAndPostId(usuarioId, postId);
        boolean esNuevaReaccion = false;

        if (reaccionExistente.isPresent()) {
            Reaccion reaccion = reaccionExistente.get();
            if (reaccion.getTipo().equals(tipo)) {
                reaccionRepository.delete(reaccion);
                log.info("Reacción eliminada para usuario {} en post {}", usuarioId, postId);
            } else {
                reaccion.setTipo(tipo);
                reaccionRepository.save(reaccion);
                esNuevaReaccion = true;
                log.info("Reacción actualizada a {} para usuario {} en post {}", tipo, usuarioId, postId);
            }
        } else {
            Reaccion nuevaReaccion = Reaccion.builder()
                    .usuario(usuario)
                    .post(post)
                    .tipo(tipo)
                    .build();
            reaccionRepository.save(nuevaReaccion);
            esNuevaReaccion = true;
            log.info("Nueva reacción {} creada para usuario {} en post {}", tipo, usuarioId, postId);
        }

        // Notificación de like
        if (esNuevaReaccion && !usuarioId.equals(post.getAutor().getId())) {
            notificationService.crearNotificacionLike(usuarioId, postId);
        }

        // Consultar directo a BD (el caché ya fue invalidado arriba)
        return calcularReaccionesPostDesdeDB(postId);
    }

    // ============================================================
    // REACCIONES A COMENTARIOS (escritura)
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_REACCIONES_COMENTARIO, key = "#comentarioId"),
            @CacheEvict(value = CACHE_USUARIO_REACCIONO_COMENTARIO,
                    key = "#usuarioId + ':' + #comentarioId"),
            @CacheEvict(value = "comments", allEntries = true)
    })
    public Map<String, Long> reaccionarComentario(Long usuarioId, Long comentarioId, String tipo) {
        log.info("Usuario {} reaccionando a comentario {} con tipo {}", usuarioId, comentarioId, tipo);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        Optional<Reaccion> reaccionExistente = reaccionRepository
                .findByUsuarioIdAndComentarioId(usuarioId, comentarioId);

        if (reaccionExistente.isPresent()) {
            Reaccion reaccion = reaccionExistente.get();
            if (reaccion.getTipo().equals(tipo)) {
                reaccionRepository.delete(reaccion);
                log.info("Reacción eliminada para usuario {} en comentario {}", usuarioId, comentarioId);
            } else {
                reaccion.setTipo(tipo);
                reaccionRepository.save(reaccion);
                log.info("Reacción actualizada a {} para usuario {} en comentario {}", tipo, usuarioId, comentarioId);
            }
        } else {
            Reaccion nuevaReaccion = Reaccion.builder()
                    .usuario(usuario)
                    .comentario(comentario)
                    .tipo(tipo)
                    .build();
            reaccionRepository.save(nuevaReaccion);
            log.info("Nueva reacción {} creada para usuario {} en comentario {}", tipo, usuarioId, comentarioId);
        }

        return calcularReaccionesComentarioDesdeDB(comentarioId);
    }

    // ============================================================
    // OBTENER REACCIONES (lectura cacheada)
    // ============================================================
    @Cacheable(value = CACHE_REACCIONES_POST, key = "#postId")
    public Map<String, Long> obtenerReaccionesPost(Long postId) {
        log.debug("[DB] Calculando reacciones del post: {}", postId);
        return calcularReaccionesPostDesdeDB(postId);
    }

    @Cacheable(value = CACHE_REACCIONES_COMENTARIO, key = "#comentarioId")
    public Map<String, Long> obtenerReaccionesComentario(Long comentarioId) {
        log.debug("[DB] Calculando reacciones del comentario: {}", comentarioId);
        return calcularReaccionesComentarioDesdeDB(comentarioId);
    }

    // ============================================================
    // HELPERS — cálculo directo desde BD
    // ============================================================
    private Map<String, Long> calcularReaccionesPostDesdeDB(Long postId) {
        Map<String, Long> reacciones = new HashMap<>();
        reacciones.put("likes", 0L);
        reacciones.put("loves", 0L);
        reacciones.put("hahas", 0L);
        reacciones.put("wows", 0L);
        reacciones.put("sads", 0L);
        reacciones.put("angrys", 0L);

        List<Object[]> resultados = reaccionRepository.countReaccionesByPost(postId);
        for (Object[] resultado : resultados) {
            String tipo = (String) resultado[0];
            Long count = (Long) resultado[1];
            switch (tipo) {
                case "like" -> reacciones.put("likes", count);
                case "love" -> reacciones.put("loves", count);
                case "haha" -> reacciones.put("hahas", count);
                case "wow" -> reacciones.put("wows", count);
                case "sad" -> reacciones.put("sads", count);
                case "angry" -> reacciones.put("angrys", count);
            }
        }
        return reacciones;
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

    // ============================================================
    // VERIFICAR SI USUARIO REACCIONÓ (lectura cacheada)
    // ============================================================
    @Cacheable(
            value = CACHE_USUARIO_REACCIONO_POST,
            key = "#usuarioId + ':' + #postId"
    )
    public boolean usuarioReaccionoPost(Long usuarioId, Long postId) {
        log.debug("[DB] Verificando si usuario {} reaccionó al post {}", usuarioId, postId);
        return reaccionRepository.findByUsuarioIdAndPostId(usuarioId, postId).isPresent();
    }

    @Cacheable(
            value = CACHE_USUARIO_REACCIONO_COMENTARIO,
            key = "#usuarioId + ':' + #comentarioId"
    )
    public boolean usuarioReaccionoComentario(Long usuarioId, Long comentarioId) {
        log.debug("[DB] Verificando si usuario {} reaccionó al comentario {}", usuarioId, comentarioId);
        return reaccionRepository.findByUsuarioIdAndComentarioId(usuarioId, comentarioId).isPresent();
    }
}