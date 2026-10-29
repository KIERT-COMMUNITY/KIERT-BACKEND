// src/main/java/com/kiert/backend/features/compartidos/service/CompartidoService.java
package com.kiert.backend.features.compartidos.service;

import com.kiert.backend.features.compartidos.dto.CompartidoDTO;
import com.kiert.backend.features.post.dto.CompartirPostDTO;
import com.kiert.backend.entity.Compartido;
import com.kiert.backend.entity.Post;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.compartidos.repository.CompartidoRepository;
import com.kiert.backend.features.post.repository.PostRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompartidoService {

    private final CompartidoRepository compartidoRepository;
    private final PostRepository postRepository;
    private final UsuarioRepository usuarioRepository;

    private static final String CACHE_COMPARTIDOS = "compartidos";
    private static final String CACHE_CONTADOR_COMPARTIDOS = "contadorCompartidos";

    private static final List<String> TIPOS_VALIDOS = List.of("INTERNO", "EXTERNO");
    private static final int MAX_COMENTARIO = 500;
    private static final long VENTANA_ANTI_SPAM_SEGUNDOS = 600;

    // ============================================================
    // COMPARTIR
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_COMPARTIDOS, key = "'post:' + #postId"),
            @CacheEvict(value = CACHE_CONTADOR_COMPARTIDOS, key = "'post:' + #postId"),
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", key = "#postId")
    })
    public CompartidoDTO compartir(Long usuarioId, Long postId, CompartirPostDTO dto) {
        log.info("Compartiendo post {} por usuario {}", postId, usuarioId);

        if (usuarioId == null || postId == null) {
            throw new BadRequestException("Parametros invalidos");
        }
        if (dto == null) {
            throw new BadRequestException("Datos de compartido requeridos");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        if (post.isEliminado()) {
            throw new BadRequestException("El post ya no esta disponible");
        }

        String tipo = dto.tipoCompartido() != null
                ? dto.tipoCompartido().trim().toUpperCase()
                : "INTERNO";

        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new BadRequestException("Tipo de compartido invalido");
        }

        String comentario = dto.comentario();
        if (comentario != null && comentario.length() > MAX_COMENTARIO) {
            throw new BadRequestException("El comentario es demasiado largo");
        }

        Instant desde = Instant.now().minusSeconds(VENTANA_ANTI_SPAM_SEGUNDOS);
        if (compartidoRepository.compartioRecientemente(usuarioId, postId, desde)) {
            throw new BadRequestException(
                    "Ya compartiste este post recientemente. Intenta mas tarde.");
        }

        Compartido compartido = Compartido.builder()
                .usuario(usuario)
                .post(post)
                .tipoCompartido(tipo)
                .comentario(comentario != null ? comentario.trim() : null)
                .fechaCreacion(Instant.now())
                .build();

        compartido = compartidoRepository.save(compartido);

        // Actualizar contador desnormalizado de forma segura
        long totalActual = post.getTotalCompartidos() != null ? post.getTotalCompartidos() : 0L;
        post.setTotalCompartidos(totalActual + 1);
        postRepository.save(post);

        log.info("Post compartido con ID: {}", compartido.getId());

        return mapearADTO(compartido);
    }

    // ============================================================
    // LISTAR COMPARTIDOS POR POST
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_COMPARTIDOS, key = "'post:' + #postId")
    public List<CompartidoDTO> listarPorPost(Long postId) {
        log.debug("[DB] Listando compartidos del post: {}", postId);
        return compartidoRepository.findByPostId(postId)
                .stream().map(this::mapearADTO).toList();
    }

    // ============================================================
    // CONTAR COMPARTIDOS
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_CONTADOR_COMPARTIDOS, key = "'post:' + #postId")
    public long contarCompartidos(Long postId) {
        log.debug("[DB] Obteniendo contador de compartidos del post: {}", postId);

        return postRepository.findById(postId)
                .map(p -> p.getTotalCompartidos() != null
                        ? p.getTotalCompartidos().longValue()
                        : 0L)
                .orElse(0L);
    }

    /**
     * Cuenta directa desde la tabla compartidos (sin cache).
     * Usar solo para reconciliacion o tareas administrativas.
     */
    @Transactional(readOnly = true)
    public long contarCompartidosDesdeDB(Long postId) {
        return compartidoRepository.countByPostId(postId);
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private CompartidoDTO mapearADTO(Compartido c) {
        return new CompartidoDTO(
                c.getId(),
                c.getUsuario() != null ? c.getUsuario().getId() : null,
                c.getUsuario() != null ? c.getUsuario().getNombreUsuario() : null,
                c.getUsuario() != null ? c.getUsuario().getFotoPerfilUrl() : null,
                c.getPost() != null ? c.getPost().getId() : null,
                c.getPost() != null ? c.getPost().getTitulo() : null,
                c.getTipoCompartido(),
                c.getComentario(),
                c.getFechaCreacion()
        );
    }
}