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

    @Transactional
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

        // Actualizar contador
        post.setTotalCompartidos(post.getTotalCompartidos() + 1);
        postRepository.save(post);

        log.info("✅ Post compartido con ID: {}", compartido.getId());

        return mapearADTO(compartido);
    }

    @Transactional(readOnly = true)
    public List<CompartidoDTO> listarPorPost(Long postId) {
        return compartidoRepository.findByPostId(postId)
                .stream().map(this::mapearADTO).toList();
    }

    @Transactional(readOnly = true)
    public long contarCompartidos(Long postId) {
        return compartidoRepository.countByPostId(postId);
    }

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