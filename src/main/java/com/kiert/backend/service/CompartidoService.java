package com.kiert.backend.service;

import com.kiert.backend.dto.CompartidoDTO;
import com.kiert.backend.dto.CompartirPostDTO;
import com.kiert.backend.entity.Compartido;
import com.kiert.backend.entity.Post;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.CompartidoRepository;
import com.kiert.backend.repository.PostRepository;
import com.kiert.backend.repository.UsuarioRepository;
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

    // Nombres de caché centralizados
    private static final String CACHE_COMPARTIDOS = "compartidos";
    private static final String CACHE_CONTADOR_COMPARTIDOS = "contadorCompartidos";

    // ============================================================
    // COMPARTIR (escritura)
    // ============================================================
    @Transactional
    @Caching(evict = {
            // Invalidar lista de compartidos del post
            @CacheEvict(value = CACHE_COMPARTIDOS, key = "'post:' + #postId"),
            // Invalidar contador del post
            @CacheEvict(value = CACHE_CONTADOR_COMPARTIDOS, key = "'post:' + #postId"),
            // Invalidar caché de posts para que se refleje el nuevo contador
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", key = "#postId")
    })
    public CompartidoDTO compartir(Long usuarioId, Long postId, CompartirPostDTO dto) {
        log.info("🔗 Compartiendo post {} por usuario {}", postId, usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        String tipo = dto.tipoCompartido() != null ? dto.tipoCompartido() : "INTERNO";
        if (!tipo.equals("INTERNO") && !tipo.equals("EXTERNO")) {
            throw new IllegalArgumentException("Tipo de compartido inválido");
        }

        Compartido compartido = Compartido.builder()
                .usuario(usuario)
                .post(post)
                .tipoCompartido(tipo)
                .comentario(dto.comentario())
                .fechaCreacion(Instant.now())
                .build();

        compartido = compartidoRepository.save(compartido);

        // Actualizar contador desnormalizado (fuente de verdad rápida)
        post.setTotalCompartidos(post.getTotalCompartidos() + 1);
        postRepository.save(post);

        log.info("✅ Post compartido con ID: {}", compartido.getId());

        return mapearADTO(compartido);
    }

    // ============================================================
    // LISTAR COMPARTIDOS POR POST (lectura frecuente)
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_COMPARTIDOS,
            key = "'post:' + #postId"
    )
    public List<CompartidoDTO> listarPorPost(Long postId) {
        log.info("📋 [DB] Listando compartidos del post: {}", postId);
        return compartidoRepository.findByPostId(postId)
                .stream().map(this::mapearADTO).toList();
    }

    // ============================================================
    // CONTAR COMPARTIDOS (lectura muy frecuente — N+1 en feeds)
    // ============================================================
    /**
     * ⚠️ IMPORTANTE: Este método usa el contador desnormalizado `post.totalCompartidos`
     * en lugar de un COUNT(*) a la tabla compartidos, porque es mucho más rápido
     * y se mantiene sincronizado por el método compartir().
     *
     * Si necesitas el conteo exacto desde la tabla (para auditoría), usa
     * contarCompartidosDesdeDB() en su lugar.
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_CONTADOR_COMPARTIDOS,
            key = "'post:' + #postId"
    )
    public long contarCompartidos(Long postId) {
        log.debug("🔍 [DB] Obteniendo contador de compartidos del post: {}", postId);

        // Usar el contador desnormalizado del post (rápido)
        return postRepository.findById(postId)
                .map(p -> p.getTotalCompartidos() != null
                        ? p.getTotalCompartidos().longValue()
                        : 0L)
                .orElse(0L);
    }

    /**
     * Conteo exacto desde la tabla compartidos.
     * Úsalo solo para auditoría o verificación, no en flujos de lectura.
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