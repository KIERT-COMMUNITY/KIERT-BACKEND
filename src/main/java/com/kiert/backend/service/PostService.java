package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.ComentarioRepository;
import com.kiert.backend.repository.PostRepository;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Espejo del backend que necesita post.service.ts: listar, obtener, crear
// publicaciones y su hilo de comentarios.
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ComentarioRepository comentarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PostMapper postMapper;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<PostDTO> listar() {
        return postRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(postMapper::aDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostDTO obtenerPorId(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación."));
        return postMapper.aDTO(post);
    }

    @Transactional
    public PostDTO crear(Long autorId, CrearPostDTO datos, List<MultipartFile> archivos) {
        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        Post post = Post.builder()
                .autor(autor)
                .titulo(datos.titulo())
                .categoria(CategoriaPost.desdeValor(datos.categoria()))
                .descripcion(datos.descripcion())
                .build();

        if (datos.link() != null && !datos.link().isBlank()) {
            post.getAdjuntos().add(Adjunto.builder()
                    .post(post)
                    .tipo(Adjunto.TipoAdjunto.LINK)
                    .nombre(datos.link())
                    .url(datos.link())
                    .build());
        }

        if (archivos != null) {
            for (MultipartFile archivo : archivos) {
                if (archivo.isEmpty()) continue;
                // Límite de 10MB por archivo, igual que valida create-post.component.ts en el frontend
                if (archivo.getSize() > 10 * 1024 * 1024) continue;

                String url = storageService.subirArchivo(archivo);
                post.getAdjuntos().add(Adjunto.builder()
                        .post(post)
                        .tipo(Adjunto.TipoAdjunto.ARCHIVO)
                        .nombre(archivo.getOriginalFilename())
                        .url(url)
                        .pesoKb((int) (archivo.getSize() / 1024))
                        .build());
            }
        }

        post = postRepository.save(post);
        return postMapper.aDTO(post);
    }

    @Transactional(readOnly = true)
    public List<ComentarioDTO> listarComentarios(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new RecursoNoEncontradoException("No se encontró la publicación.");
        }
        return comentarioRepository.findByPostIdOrderByFechaCreacionAsc(postId).stream()
                .map(postMapper::aDTO)
                .toList();
    }

    @Transactional
    public ComentarioDTO comentar(Long postId, Long autorId, String contenido) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación."));
        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        Comentario comentario = Comentario.builder()
                .post(post)
                .autor(autor)
                .contenido(contenido)
                .build();

        comentario = comentarioRepository.save(comentario);
        return postMapper.aDTO(comentario);
    }
}
