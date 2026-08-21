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

    @Transactional
    public Map<String, Long> reaccionarPost(Long usuarioId, Long postId, String tipo) {
        log.info("Usuario {} reaccionando a post {} con tipo {}", usuarioId, postId, tipo);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        // Verificar si ya existe reacción
        Optional<Reaccion> reaccionExistente = reaccionRepository.findByUsuarioIdAndPostId(usuarioId, postId);

        if (reaccionExistente.isPresent()) {
            Reaccion reaccion = reaccionExistente.get();
            if (reaccion.getTipo().equals(tipo)) {
                // Si es el mismo tipo, eliminar (toggle off)
                reaccionRepository.delete(reaccion);
                log.info("Reacción eliminada para usuario {} en post {}", usuarioId, postId);
            } else {
                // Si es diferente tipo, actualizar
                reaccion.setTipo(tipo);
                reaccionRepository.save(reaccion);
                log.info("Reacción actualizada a {} para usuario {} en post {}", tipo, usuarioId, postId);
            }
        } else {
            // Nueva reacción
            Reaccion nuevaReaccion = Reaccion.builder()
                    .usuario(usuario)
                    .post(post)
                    .tipo(tipo)
                    .build();
            reaccionRepository.save(nuevaReaccion);
            log.info("Nueva reacción {} creada para usuario {} en post {}", tipo, usuarioId, postId);
        }

        return obtenerReaccionesPost(postId);
    }

    @Transactional
    public Map<String, Long> reaccionarComentario(Long usuarioId, Long comentarioId, String tipo) {
        log.info("Usuario {} reaccionando a comentario {} con tipo {}", usuarioId, comentarioId, tipo);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Comentario comentario = comentarioRepository.findById(comentarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));

        Optional<Reaccion> reaccionExistente = reaccionRepository.findByUsuarioIdAndComentarioId(usuarioId, comentarioId);

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

        return obtenerReaccionesComentario(comentarioId);
    }

    public Map<String, Long> obtenerReaccionesPost(Long postId) {
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
                case "like": reacciones.put("likes", count); break;
                case "love": reacciones.put("loves", count); break;
                case "haha": reacciones.put("hahas", count); break;
                case "wow": reacciones.put("wows", count); break;
                case "sad": reacciones.put("sads", count); break;
                case "angry": reacciones.put("angrys", count); break;
            }
        }
        return reacciones;
    }

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

    public boolean usuarioReaccionoPost(Long usuarioId, Long postId) {
        return reaccionRepository.findByUsuarioIdAndPostId(usuarioId, postId).isPresent();
    }

    public boolean usuarioReaccionoComentario(Long usuarioId, Long comentarioId) {
        return reaccionRepository.findByUsuarioIdAndComentarioId(usuarioId, comentarioId).isPresent();
    }
}