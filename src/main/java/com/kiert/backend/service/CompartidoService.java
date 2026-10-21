// src/main/java/com/kiert/backend/service/CompartidoService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.CompartidoDTO;
import com.kiert.backend.dto.CompartirPostDTO;
import com.kiert.backend.entity.Compartido;
import com.kiert.backend.entity.Post;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.BadRequestException;
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

    private static final String CACHE_COMPARTIDOS = "compartidos";
    private static final String CACHE_CONTADOR_COMPARTIDOS = "contadorCompartidos";

    private static final List<String> TIPOS_VALIDOS = List.of("INTERNO", "EXTERNO");
    private static final int MAX_COMENTARIO = 500;

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

        // Validaciones básicas
        if (usuarioId == null || postId == null) {
            throw new BadRequestException("Parámetros inválidos");
        }
        if (dto == null) {
            throw new BadRequestException("Datos de compartido requeridos");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));

        if (post.isEliminado()) {
            throw new BadRequestException("El post ya no está disponible");
        }

        // Validar tipo
        String tipo = dto.tipoCompartido() != null
                ? dto.tipoCompartido().trim().toUpperCase()
                : "INTERNO";

        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new BadRequestException("Tipo de compartido inválido");
        }

        // Validar comentario
        String comentario = dto.comentario();
        if (comentario != null && comentario.length() > MAX_COMENTARIO) {
            throw new BadRequestException("El comentario es demasiado largo");
        }

        // Anti-spam: verificar si el usuario ya compartió el post recientemente
        Instant hace10Minutos = Instant.now().minusSeconds(600);
        if (compartidoRepository.compartioRecientemente(usuarioId, postId, hace10Minutos)) {
            throw new BadRequestException(
                    "Ya compartiste este post recientemente. Intenta más tarde."
            );
        }

        Compartido compartido = Compartido.builder()
                .usuario(usuario)
                .post(post)
                .tipoCompartido(tipo)
                .comentario(comentario != null ? comentario.trim() : null)
                .fechaCreacion(Instant.now())
                .build();

        compartido = compartidoRepository.save(compartido);

        // Actualizar contador desnormalizado
        post.setTotalCompartidos(post.getTotalCompartidos() + 1);
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
        log.info("[DB] Listando compartidos del post: {}", postId);
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