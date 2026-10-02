// src/main/java/com/kiert/backend/service/PostService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.mapper.PostMapper;
import com.kiert.backend.repository.ComentarioRepository;
import com.kiert.backend.repository.PostRepository;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ComentarioRepository comentarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PostMapper postMapper;
    private final CloudinaryService cloudinaryService;

    // Nombres de caché
    private static final String CACHE_POSTS = "posts";
    private static final String CACHE_POST = "post";
    private static final String CACHE_COMMENTS = "comments";

    // ============================================================
    // LISTAR POSTS
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_POSTS, key = "'all'")
    public List<PostDTO> listar() {
        log.info("📋 [DB] Listando posts");
        try {
            List<Post> posts = postRepository.findAllActiveOrderByFechaCreacionDesc();
            log.info("✅ Se encontraron {} posts", posts.size());
            List<PostDTO> result = posts.stream()
                    .map(postMapper::aDTO)
                    .toList();
            log.info("✅ Mapeados {} posts a DTO", result.size());
            return result;
        } catch (Exception e) {
            log.error("❌ Error al listar posts: {}", e.getMessage(), e);
            throw new RuntimeException("Error al listar publicaciones: " + e.getMessage());
        }
    }

    // ============================================================
    // OBTENER POST POR ID
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_POST, key = "#id")
    public PostDTO obtenerPorId(Long id) {
        log.info("🔍 [DB] Obteniendo post {}", id);

        try {
            if (!postRepository.existsById(id)) {
                log.warn("⚠️ Post con ID {} no existe en la base de datos", id);
                throw new RecursoNoEncontradoException("No se encontró la publicación con ID: " + id);
            }

            Optional<Post> postOpt = postRepository.findActiveById(id);

            if (postOpt.isEmpty()) {
                log.warn("⚠️ Post con ID {} está eliminado o no existe", id);
                throw new RecursoNoEncontradoException("No se encontró la publicación con ID: " + id);
            }

            Post post = postOpt.get();
            log.info("✅ Post encontrado: ID={}, Título={}, Categoría={}, Autor={}",
                    post.getId(), post.getTitulo(), post.getCategoria(),
                    post.getAutor() != null ? post.getAutor().getNombreUsuario() : "null");

            PostDTO dto = postMapper.aDTO(post);
            log.info("✅ DTO mapeado: Categoría={}", dto.categoria());

            return dto;

        } catch (RecursoNoEncontradoException e) {
            log.warn("⚠️ Post no encontrado: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error al obtener post {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error al obtener publicación: " + e.getMessage());
        }
    }

    // ============================================================
    // CREAR POST
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_POSTS, allEntries = true),
            @CacheEvict(value = CACHE_POST, allEntries = true)
    })
    public PostDTO crear(Long autorId, CrearPostDTO datos, List<MultipartFile> archivos) {
        log.info("📝 Creando post para usuario: {}", autorId);
        log.info("📄 Título: {}", datos.titulo());
        log.info("📂 Categoría ingresada por el usuario: '{}'", datos.categoria());

        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));

        String categoriaUsuario = datos.categoria() != null ? datos.categoria().trim() : "otro";
        log.info("📌 Guardando categoría: '{}'", categoriaUsuario);

        Post post = Post.builder()
                .autor(autor)
                .titulo(datos.titulo())
                .categoria(categoriaUsuario)
                .descripcion(datos.descripcion())
                .eliminado(false)
                .fechaCreacion(Instant.now())
                .build();

        log.info("📦 Post a guardar: Título={}, Categoría={}", post.getTitulo(), post.getCategoria());

        if (datos.link() != null && !datos.link().isBlank()) {
            log.info("🔗 Agregando link: {}", datos.link());
            Adjunto adjunto = Adjunto.builder()
                    .post(post)
                    .tipo("link")
                    .nombre("Enlace externo")
                    .url(datos.link())
                    .build();
            post.getAdjuntos().add(adjunto);
        }

        post = postRepository.save(post);
        log.info("✅ Post guardado con ID: {}, Categoría: '{}'", post.getId(), post.getCategoria());

        postRepository.flush();

        if (archivos != null && !archivos.isEmpty()) {
            log.info("📎 Procesando {} archivo(s)", archivos.size());

            for (MultipartFile archivo : archivos) {
                if (archivo.isEmpty()) {
                    log.warn("⚠️ Archivo vacío, saltando...");
                    continue;
                }

                try {
                    String url;
                    String tipo;
                    String carpeta = "posts";
                    Integer duracionSegundos = null;
                    String formato = cloudinaryService.getFormato(archivo.getContentType());

                    if (cloudinaryService.esVideo(archivo)) {
                        log.info("🎥 Subiendo video: {}", archivo.getOriginalFilename());
                        Map<String, Object> result = cloudinaryService.subirVideo(archivo, carpeta + "/videos");
                        url = result.get("secure_url").toString();
                        tipo = "video";
                        if (result.containsKey("duration")) {
                            duracionSegundos = ((Number) result.get("duration")).intValue();
                        }
                    } else if (cloudinaryService.esGif(archivo)) {
                        log.info("🎬 Subiendo GIF: {}", archivo.getOriginalFilename());
                        url = cloudinaryService.subirGif(archivo, carpeta + "/gifs");
                        tipo = "gif";
                    } else if (cloudinaryService.esImagen(archivo)) {
                        log.info("🖼️ Subiendo imagen: {}", archivo.getOriginalFilename());
                        url = cloudinaryService.subirImagen(archivo, carpeta + "/imagenes");
                        tipo = "imagen";
                    } else {
                        log.info("📎 Subiendo archivo: {}", archivo.getOriginalFilename());
                        url = cloudinaryService.subirArchivo(archivo, carpeta + "/archivos");
                        tipo = "archivo";
                    }

                    Adjunto adjunto = Adjunto.builder()
                            .post(post)
                            .tipo(tipo)
                            .nombre(archivo.getOriginalFilename())
                            .url(url)
                            .pesoKb((int) (archivo.getSize() / 1024))
                            .formato(formato)
                            .duracionSegundos(duracionSegundos)
                            .build();

                    post.getAdjuntos().add(adjunto);
                    log.info("✅ {} subido exitosamente", tipo);

                } catch (Exception e) {
                    log.error("❌ Error al subir archivo {}: {}", archivo.getOriginalFilename(), e.getMessage());
                }
            }

            post = postRepository.save(post);
            log.info("✅ Post actualizado con {} adjunto(s)", post.getAdjuntos().size());
            postRepository.flush();
        }

        log.info("✅ Post completado exitosamente con ID: {}, Categoría: '{}'", post.getId(), post.getCategoria());
        return postMapper.aDTO(post);
    }

    // ============================================================
    // LISTAR COMENTARIOS
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_COMMENTS, key = "#postId")
    public List<ComentarioDTO> listarComentarios(Long postId) {
        log.info("💬 [DB] Listando comentarios del post {}", postId);
        if (!postRepository.existsById(postId)) {
            throw new RecursoNoEncontradoException("No se encontró la publicación.");
        }
        return comentarioRepository.findByPostIdAndEliminadoFalseOrderByFechaCreacionAsc(postId).stream()
                .map(postMapper::aDTO)
                .toList();
    }

    // ============================================================
    // COMENTAR
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_COMMENTS, key = "#postId"),
            @CacheEvict(value = CACHE_POST, key = "#postId"),
            @CacheEvict(value = CACHE_POSTS, allEntries = true)
    })
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

    // ============================================================
    // ELIMINAR POST
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_POSTS, allEntries = true),
            @CacheEvict(value = CACHE_POST, key = "#postId"),
            @CacheEvict(value = CACHE_COMMENTS, key = "#postId")
    })
    public void eliminarPost(Long postId, Long autorId) {
        log.info("🗑️ Eliminando post {} por usuario {}", postId, autorId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación."));

        if (!post.getAutor().getId().equals(autorId)) {
            throw new SecurityException("No tienes permiso para eliminar esta publicación.");
        }

        post.setEliminado(true);
        post.setFechaEliminacion(Instant.now());
        postRepository.save(post);
        log.info("✅ Post {} eliminado exitosamente", postId);
    }
}