// src/main/java/com/kiert/backend/features/reacciones/service/ReaccionService.java
package com.kiert.backend.features.reacciones.service;

import com.kiert.backend.entity.Reaccion;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.entity.Post;
import com.kiert.backend.entity.Comentario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.reacciones.dto.ReaccionesDTO;
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

import java.util.List;
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
    private static final String CACHE_COMENTARIOS = "comentarios";

    private static final Set<String> TIPOS_POST = Set.of(
            "like", "love", "haha", "wow", "sad", "angry"
    );

    private static final Set<String> TIPOS_COMENTARIO = Set.of(
            "like", "love"
    );

    // ============================================================
    // REACCIONES A POSTS
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_REACCIONES_POST, key = "#postId", beforeInvocation = true),
            @CacheEvict(value = CACHE_USUARIO_REACCIONO_POST,
                    key = "#usuarioId + ':' + #postId", beforeInvocation = true),
            @CacheEvict(value = "posts", allEntries = true, beforeInvocation = true),
            @CacheEvict(value = "post", key = "#postId", beforeInvocation = true)
    })
    public ReaccionesDTO reaccionarPost(Long usuarioId, Long postId, String tipo) {
        log.debug("Usuario {} reaccionando a post {} con tipo {}", usuarioId, postId, tipo);

        if (usuarioId == null || postId == null) {
            throw new BadRequestException("Parametros invalidos");
        }
        if (tipo == null || !TIPOS_POST.contains(tipo)) {
            throw new BadRequestException("Tipo de reaccion invalido");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        if (post.isEliminado()) {
            throw new BadRequestException("La publicacion ya no esta disponible");
        }

        Optional<Reaccion> reaccionExistente = reaccionRepository.findByUsuarioIdAndPostId(usuarioId, postId);
        boolean esNuevaReaccion = false;

        if (reaccionExistente.isPresent()) {
            Reaccion reaccion = reaccionExistente.get();
            if (reaccion.getTipo().equals(tipo)) {
                reaccionRepository.delete(reaccion);
                log.debug("Reaccion eliminada para usuario {} en post {}", usuarioId, postId);
            } else {
                reaccion.setTipo(tipo);
                reaccionRepository.save(reaccion);
                esNuevaReaccion = true;
                log.debug("Reaccion actualizada a {} para usuario {} en post {}", tipo, usuarioId, postId);
            }
        } else {
            Reaccion nuevaReaccion = Reaccion.builder()
                    .usuario(usuario)
                    .post(post)
                    .tipo(tipo)
                    .build();
            reaccionRepository.save(nuevaReaccion);
            esNuevaReaccion = true;
            log.debug("Nueva reaccion {} creada para usuario {} en post {}", tipo, usuarioId, postId);
        }

        if (esNuevaReaccion && "like".equals(tipo) && !usuarioId.equals(post.getAutor().getId())) {
            try {
                notificationService.crearNotificacionLike(usuarioId, postId);
            } catch (Exception e) {
                log.warn("Error creando notificacion: {}", e.getMessage());
            }
        }

        return calcularReaccionesPostDesdeDB(postId);
    }

    // ============================================================
    // REACCIONES A COMENTARIOS
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_REACCIONES_COMENTARIO,
                    key = "'comentario:' + #comentarioId", beforeInvocation = true),
            @CacheEvict(value = CACHE_USUARIO_REACCIONO_COMENTARIO,
                    key = "#usuarioId + ':' + #comentarioId", beforeInvocation = true),
            @CacheEvict(value = CACHE_COMENTARIOS, allEntries = true, beforeInvocation = true)
    })
    public ReaccionesDTO reaccionarComentario(Long usuarioId, Long comentarioId, String tipo) {
        log.debug("Usuario {} reaccionando a comentario {} con tipo {}", usuarioId, comentarioId, tipo);

        if (usuarioId == null || comentarioId == null) {
            throw new BadRequestException("Parametros invalidos");
        }
        if (tipo == null || !TIPOS_COMENTARIO.contains(tipo)) {
            throw new BadRequestException("Tipo de reaccion invalido");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Comentario comentario = comentarioRepository.findActivoConAutor(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        if (comentario.isEliminado()) {
            throw new BadRequestException("El comentario ya no esta disponible");
        }

        Optional<Reaccion> reaccionExistente = reaccionRepository
                .findByUsuarioIdAndComentarioId(usuarioId, comentarioId);

        if (reaccionExistente.isPresent()) {
            Reaccion reaccion = reaccionExistente.get();
            if (reaccion.getTipo().equals(tipo)) {
                reaccionRepository.delete(reaccion);
                log.debug("Reaccion eliminada para usuario {} en comentario {}", usuarioId, comentarioId);
            } else {
                reaccion.setTipo(tipo);
                reaccionRepository.save(reaccion);
                log.debug("Reaccion actualizada a {} para usuario {} en comentario {}", tipo, usuarioId, comentarioId);
            }
        } else {
            Reaccion nuevaReaccion = Reaccion.builder()
                    .usuario(usuario)
                    .comentario(comentario)
                    .tipo(tipo)
                    .build();
            reaccionRepository.save(nuevaReaccion);
            log.debug("Nueva reaccion {} creada para usuario {} en comentario {}", tipo, usuarioId, comentarioId);
        }

        return calcularReaccionesComentarioDesdeDB(comentarioId);
    }

    // ============================================================
    // OBTENER REACCIONES
    // ============================================================
    @Cacheable(value = CACHE_REACCIONES_POST, key = "#postId")
    public ReaccionesDTO obtenerReaccionesPost(Long postId) {
        log.debug("[DB] Calculando reacciones del post: {}", postId);
        return calcularReaccionesPostDesdeDB(postId);
    }

    @Cacheable(value = CACHE_REACCIONES_COMENTARIO, key = "'comentario:' + #comentarioId")
    public ReaccionesDTO obtenerReaccionesComentario(Long comentarioId) {
        log.debug("[DB] Calculando reacciones del comentario: {}", comentarioId);
        return calcularReaccionesComentarioDesdeDB(comentarioId);
    }

    // ============================================================
    // VERIFICAR SI USUARIO REACCIONO
    // ============================================================
    @Cacheable(value = CACHE_USUARIO_REACCIONO_POST, key = "#usuarioId + ':' + #postId")
    public boolean usuarioReaccionoPost(Long usuarioId, Long postId) {
        log.debug("[DB] Verificando si usuario {} reacciono al post {}", usuarioId, postId);
        if (usuarioId == null || postId == null) return false;
        return reaccionRepository.findByUsuarioIdAndPostId(usuarioId, postId).isPresent();
    }

    @Cacheable(value = CACHE_USUARIO_REACCIONO_COMENTARIO, key = "#usuarioId + ':' + #comentarioId")
    public boolean usuarioReaccionoComentario(Long usuarioId, Long comentarioId) {
        log.debug("[DB] Verificando si usuario {} reacciono al comentario {}", usuarioId, comentarioId);
        if (usuarioId == null || comentarioId == null) return false;
        return reaccionRepository.findByUsuarioIdAndComentarioId(usuarioId, comentarioId).isPresent();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private ReaccionesDTO calcularReaccionesPostDesdeDB(Long postId) {
        long likes = 0L, loves = 0L, hahas = 0L, wows = 0L, sads = 0L, angrys = 0L;

        List<Object[]> resultados = reaccionRepository.countReaccionesByPost(postId);
        if (resultados == null || resultados.isEmpty()) {
            return new ReaccionesDTO(likes, loves, hahas, wows, sads, angrys);
        }
        for (Object[] resultado : resultados) {
            if (resultado == null || resultado.length < 2) continue;

            String tipo = String.valueOf(resultado[0]);
            Number countNum = (resultado[1] instanceof Number) ? (Number) resultado[1] : 0L;
            long count = countNum.longValue();

            switch (tipo) {
                case "like" -> likes = count;
                case "love" -> loves = count;
                case "haha" -> hahas = count;
                case "wow" -> wows = count;
                case "sad" -> sads = count;
                case "angry" -> angrys = count;
                default -> log.warn("Tipo de reaccion desconocido en DB: {}", tipo);
            }
        }
        return new ReaccionesDTO(likes, loves, hahas, wows, sads, angrys);
    }

    private ReaccionesDTO calcularReaccionesComentarioDesdeDB(Long comentarioId) {
        long likes = 0L, loves = 0L;

        List<Object[]> resultados = reaccionRepository.countReaccionesByComentario(comentarioId);
        if (resultados == null || resultados.isEmpty()) {
            return new ReaccionesDTO(likes, loves, 0L, 0L, 0L, 0L);
        }
        for (Object[] resultado : resultados) {
            if (resultado == null || resultado.length < 2) continue;

            String tipo = String.valueOf(resultado[0]);
            Number countNum = (resultado[1] instanceof Number) ? (Number) resultado[1] : 0L;
            long count = countNum.longValue();

            switch (tipo) {
                case "like" -> likes = count;
                case "love" -> loves = count;
                default -> log.warn("Tipo de reaccion desconocido en DB: {}", tipo);
            }
        }
        return new ReaccionesDTO(likes, loves, 0L, 0L, 0L, 0L);
    }
}