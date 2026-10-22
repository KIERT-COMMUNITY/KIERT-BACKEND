// src/main/java/com/kiert/backend/service/ReaccionService.java
package com.kiert.backend.features.reacciones.service;

import com.kiert.backend.entity.Reaccion;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.entity.Post;
import com.kiert.backend.entity.Comentario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.reacciones.repository.ReaccionRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import com.kiert.backend.features.post.repository.PostRepository;
import com.kiert.backend.features.comentarios.repository.ComentarioRepository;
import com.kiert.backend.features.notificaciones.service.NotificacionService;
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
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReaccionService {

    private final ReaccionRepository reaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PostRepository postRepository;
    private final ComentarioRepository comentarioRepository;
    private final NotificacionService notificationService;

    private static final String CACHE_REACCIONES_POST = "reaccionesPost";
    private static final String CACHE_REACCIONES_COMENTARIO = "reaccionesComentario";
    private static final String CACHE_USUARIO_REACCIONO_POST = "usuarioReaccionoPost";
    private static final String CACHE_USUARIO_REACCIONO_COMENTARIO = "usuarioReaccionoComentario";

    // Whitelist de tipos de reacción válidos (para posts)
    private static final Set<String> TIPOS_POST = Set.of(
            "like", "love", "haha", "wow", "sad", "angry"
    );

    // Whitelist de tipos de reacción válidos (para comentarios)
    private static final Set<String> TIPOS_COMENTARIO = Set.of(
            "like", "love"
    );

    // ============================================================
    // REACCIONES A POSTS
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_REACCIONES_POST, key = "#postId"),
            @CacheEvict(value = CACHE_USUARIO_REACCIONO_POST,
                    key = "#usuarioId + ':' + #postId"),
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", key = "#postId")
    })
    public Map<String, Long> reaccionarPost(Long usuarioId, Long postId, String tipo) {
        log.info("Usuario {} reaccionando a post {} con tipo {}", usuarioId, postId, tipo);

        // Validaciones
        if (usuarioId == null || postId == null) {
            throw new BadRequestException("Parámetros inválidos");
        }
        if (tipo == null || !TIPOS_POST.contains(tipo)) {
            throw new BadRequestException("Tipo de reacción inválido");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        if (post.isEliminado()) {
            throw new BadRequestException("La publicación ya no está disponible");
        }

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

        // Notificación de like (solo para likes, no para los demás)
        if (esNuevaReaccion && "like".equals(tipo) && !usuarioId.equals(post.getAutor().getId())) {
            try {
                notificationService.crearNotificacionLike(usuarioId, postId);
            } catch (Exception e) {
                log.warn("Error creando notificación: {}", e.getMessage());
            }
        }

        return calcularReaccionesPostDesdeDB(postId);
    }

    // ============================================================
    // REACCIONES A COMENTARIOS
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

        // Validaciones
        if (usuarioId == null || comentarioId == null) {
            throw new BadRequestException("Parámetros inválidos");
        }
        if (tipo == null || !TIPOS_COMENTARIO.contains(tipo)) {
            throw new BadRequestException("Tipo de reacción inválido");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Comentario comentario = comentarioRepository.findActivoConAutor(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        if (comentario.isEliminado()) {
            throw new BadRequestException("El comentario ya no está disponible");
        }

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
    // OBTENER REACCIONES
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
    // HELPERS
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
    // VERIFICAR SI USUARIO REACCIONÓ
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