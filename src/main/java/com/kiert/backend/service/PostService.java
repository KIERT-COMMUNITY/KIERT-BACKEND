package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.ComentarioRepository;
import com.kiert.backend.repository.PostRepository;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ComentarioRepository comentarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PostMapper postMapper;
    private final StorageService storageService;

    // ========== LISTAR POSTS ==========
    @Transactional(readOnly = true)
    public List<PostDTO> listar() {
        log.info("📋 Listando posts desde BD");
        try {
            List<PostDTO> posts = postRepository.findAllByOrderByFechaCreacionDesc().stream()
                    .map(postMapper::aDTO)
                    .toList();
            log.info("✅ Se encontraron {} posts", posts.size());
            return posts;
        } catch (Exception e) {
            log.error("❌ Error al listar posts: {}", e.getMessage(), e);
            throw new RuntimeException("Error al listar publicaciones: " + e.getMessage());
        }
    }

    // ========== OBTENER POST POR ID ==========
    @Transactional(readOnly = true)
    public PostDTO obtenerPorId(Long id) {
        log.info("🔍 Obteniendo post {} desde BD", id);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación."));
        return postMapper.aDTO(post);
    }

    // ========== CREAR POST ==========
    @Transactional
    public PostDTO crear(Long autorId, CrearPostDTO datos, List<MultipartFile> archivos) {
        log.info("📝 Creando post para usuario: {}", autorId);
        log.info("📄 Título: {}", datos.titulo());
        log.info("📎 Archivos recibidos: {}", archivos != null ? archivos.size() : 0);

        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        Post post = Post.builder()
                .autor(autor)
                .titulo(datos.titulo())
                .categoria(CategoriaPost.desdeValor(datos.categoria()))
                .descripcion(datos.descripcion())
                .build();

        // ✅ Guardar link si existe
        if (datos.link() != null && !datos.link().isBlank()) {
            log.info("🔗 Agregando link: {}", datos.link());
            post.getAdjuntos().add(Adjunto.builder()
                    .post(post)
                    .tipo(Adjunto.TipoAdjunto.LINK)
                    .nombre("Enlace externo")
                    .url(datos.link())
                    .build());
        }

        // ✅ Guardar el post primero
        post = postRepository.save(post);
        log.info("✅ Post guardado con ID: {}", post.getId());

        // ✅ Subir archivos a Cloudinary desde el backend
        if (archivos != null && !archivos.isEmpty()) {
            log.info("📎 Procesando {} archivo(s)", archivos.size());

            for (MultipartFile archivo : archivos) {
                if (archivo.isEmpty()) {
                    log.warn("⚠️ Archivo vacío, saltando...");
                    continue;
                }

                if (archivo.getSize() > 10 * 1024 * 1024) {
                    log.warn("⚠️ Archivo {} excede 10MB, saltando...", archivo.getOriginalFilename());
                    continue;
                }

                try {
                    log.info("📤 Subiendo archivo a Cloudinary: {}", archivo.getOriginalFilename());
                    String url = storageService.subirArchivo(archivo);

                    Adjunto adjunto = Adjunto.builder()
                            .post(post)
                            .tipo(Adjunto.TipoAdjunto.ARCHIVO)
                            .nombre(archivo.getOriginalFilename())
                            .url(url)
                            .pesoKb((int) (archivo.getSize() / 1024))
                            .build();

                    post.getAdjuntos().add(adjunto);
                    log.info("✅ Archivo subido exitosamente: {}", url);

                } catch (Exception e) {
                    log.error("❌ Error al subir archivo {}: {}", archivo.getOriginalFilename(), e.getMessage());
                }
            }

            post = postRepository.save(post);
            log.info("✅ Post actualizado con {} adjunto(s)", post.getAdjuntos().size());
        }

        log.info("✅ Post completado exitosamente con ID: {}", post.getId());
        return postMapper.aDTO(post);
    }

    // ========== LISTAR COMENTARIOS ==========
    @Transactional(readOnly = true)
    public List<ComentarioDTO> listarComentarios(Long postId) {
        log.info("💬 Listando comentarios del post {} desde BD", postId);
        if (!postRepository.existsById(postId)) {
            throw new RecursoNoEncontradoException("No se encontró la publicación.");
        }
        return comentarioRepository.findByPostIdOrderByFechaCreacionAsc(postId).stream()
                .map(postMapper::aDTO)
                .toList();
    }

    // ========== COMENTAR ==========
    @Transactional
    public ComentarioDTO comentar(Long postId, Long autorId, String contenido) {
        log.info("💬 Comentando en post {} por usuario {}", postId, autorId);

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

    // ========== ELIMINAR POST ==========
    @Transactional
    public void eliminarPost(Long postId, Long autorId) {
        log.info("🗑️ Eliminando post {} por usuario {}", postId, autorId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación."));

        // Verificar que el usuario sea el autor
        if (!post.getAutor().getId().equals(autorId)) {
            throw new SecurityException("No tienes permiso para eliminar esta publicación.");
        }

        postRepository.delete(post);
        log.info("✅ Post {} eliminado exitosamente", postId);
    }

    // ========== ACTUALIZAR POST ==========
    @Transactional
    public PostDTO actualizarPost(Long postId, Long autorId, ActualizarPostDTO datos) {
        log.info("✏️ Actualizando post {} por usuario {}", postId, autorId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación."));

        // Verificar que el usuario sea el autor
        if (!post.getAutor().getId().equals(autorId)) {
            throw new SecurityException("No tienes permiso para actualizar esta publicación.");
        }

        // Actualizar campos
        if (datos.titulo() != null && !datos.titulo().isBlank()) {
            post.setTitulo(datos.titulo());
        }
        if (datos.descripcion() != null && !datos.descripcion().isBlank()) {
            post.setDescripcion(datos.descripcion());
        }
        if (datos.categoria() != null && !datos.categoria().isBlank()) {
            post.setCategoria(CategoriaPost.desdeValor(datos.categoria()));
        }

        post = postRepository.save(post);
        return postMapper.aDTO(post);
    }

    // ========== LIMPIAR CACHÉ DE POSTS ==========
    public void limpiarCachePosts() {
        log.info("🧹 Limpiando caché de posts");
        // No hace nada porque Redis está desactivado
    }
}