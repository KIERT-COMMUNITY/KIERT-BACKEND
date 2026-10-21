// src/main/java/com/kiert/backend/service/PostService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.BadRequestException;
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

    private static final String CACHE_POSTS = "posts";
    private static final String CACHE_POST = "post";
    private static final String CACHE_COMMENTS = "comments";

    private static final int MAX_TITULO = 200;
    private static final int MAX_DESCRIPCION = 5000;
    private static final int MAX_COMENTARIO = 2000;
    private static final int MAX_ARCHIVOS = 5;
    private static final long MAX_TOTAL_ARCHIVOS = 30L * 1024L * 1024L;

    // ============================================================
    // LISTAR POSTS
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_POSTS, key = "'all'")
    public List<PostDTO> listar() {
        log.info("[DB] Listando posts");
        List<Post> posts = postRepository.findAllActiveOrderByFechaCreacionDesc();
        return posts.stream()
                .map(postMapper::aDTO)
                .toList();
    }

    // ============================================================
    // OBTENER POST POR ID
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_POST, key = "#id")
    public PostDTO obtenerPorId(Long id) {
        log.info("[DB] Obteniendo post {}", id);

        if (id == null) {
            throw new BadRequestException("ID requerido");
        }

        Post post = postRepository.findActiveById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la publicación"));

        return postMapper.aDTO(post);
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
        log.info("Creando post para usuario: {}", autorId);

        // Validaciones
        if (autorId == null) {
            throw new BadRequestException("Usuario requerido");
        }
        if (datos == null) {
            throw new BadRequestException("Datos requeridos");
        }
        if (datos.titulo() == null || datos.titulo().isBlank()) {
            throw new BadRequestException("El título es obligatorio");
        }
        if (datos.titulo().length() > MAX_TITULO) {
            throw new BadRequestException("El título es demasiado largo");
        }
        if (datos.descripcion() != null && datos.descripcion().length() > MAX_DESCRIPCION) {
            throw new BadRequestException("La descripción es demasiado larga");
        }
        if (archivos != null && archivos.size() > MAX_ARCHIVOS) {
            throw new BadRequestException("Solo se permiten " + MAX_ARCHIVOS + " archivos");
        }

        // Validar tamaño total de archivos
        if (archivos != null && !archivos.isEmpty()) {
            long total = archivos.stream()
                    .filter(a -> a != null && !a.isEmpty())
                    .mapToLong(MultipartFile::getSize)
                    .sum();
            if (total > MAX_TOTAL_ARCHIVOS) {
                throw new BadRequestException("Los archivos no pueden superar 30 MB");
            }
        }

        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String categoriaUsuario = datos.categoria() != null ? datos.categoria().trim() : "otro";

        Post post = Post.builder()
                .autor(autor)
                .titulo(datos.titulo().trim())
                .categoria(categoriaUsuario)
                .descripcion(datos.descripcion())
                .eliminado(false)
                .fechaCreacion(Instant.now())
                .build();

        if (datos.link() != null && !datos.link().isBlank()) {
            Adjunto adjunto = Adjunto.builder()
                    .post(post)
                    .tipo("link")
                    .nombre("Enlace externo")
                    .url(datos.link())
                    .build();
            post.getAdjuntos().add(adjunto);
        }

        post = postRepository.save(post);
        postRepository.flush();

        if (archivos != null && !archivos.isEmpty()) {
            log.info("Procesando {} archivo(s)", archivos.size());

            for (MultipartFile archivo : archivos) {
                if (archivo == null || archivo.isEmpty()) continue;

                try {
                    String url;
                    String tipo;
                    String carpeta = "posts";
                    Integer duracionSegundos = null;
                    String formato = cloudinaryService.getFormato(archivo.getContentType());

                    if (cloudinaryService.esVideo(archivo)) {
                        Map<String, Object> result = cloudinaryService.subirVideo(archivo, carpeta + "/videos");
                        url = result.get("secure_url").toString();
                        tipo = "video";
                        if (result.containsKey("duration")) {
                            duracionSegundos = ((Number) result.get("duration")).intValue();
                        }
                    } else if (cloudinaryService.esGif(archivo)) {
                        url = cloudinaryService.subirGif(archivo, carpeta + "/gifs");
                        tipo = "gif";
                    } else if (cloudinaryService.esImagen(archivo)) {
                        url = cloudinaryService.subirImagen(archivo, carpeta + "/imagenes");
                        tipo = "imagen";
                    } else {
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

                } catch (Exception e) {
                    log.error("Error al subir archivo: {}", e.getMessage());
                    // No abortar el post por un archivo fallido
                }
            }

            post = postRepository.save(post);
            postRepository.flush();
        }

        return postMapper.aDTO(post);
    }

    // ============================================================
    // LISTAR COMENTARIOS
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_COMMENTS, key = "#postId")
    public List<ComentarioDTO> listarComentarios(Long postId) {
        log.info("[DB] Listando comentarios del post {}", postId);

        if (!postRepository.existsActiveById(postId)) {
            throw new RecursoNoEncontradoException("No se encontró la publicación");
        }

        return comentarioRepository.findByPostIdAndEliminadoFalseOrderByFechaCreacionAsc(postId)
                .stream()
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
        log.info("Comentando en post {} por usuario {}", postId, autorId);

        if (contenido == null || contenido.isBlank()) {
            throw new BadRequestException("El comentario no puede estar vacío");
        }
        if (contenido.length() > MAX_COMENTARIO) {
            throw new BadRequestException("El comentario es demasiado largo");
        }

        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación"));

        if (post.isEliminado()) {
            throw new BadRequestException("La publicación ya no está disponible");
        }

        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Comentario comentario = Comentario.builder()
                .post(post)
                .autor(autor)
                .contenido(contenido.trim())
                .eliminado(false)
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
        log.info("Eliminando post {} por usuario {}", postId, autorId);

        if (postId == null || autorId == null) {
            throw new BadRequestException("Parámetros inválidos");
        }

        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación"));

        if (!post.getAutor().getId().equals(autorId)) {
            // Mensaje genérico para no revelar existencia
            throw new SecurityException("No tienes permiso para eliminar esta publicación");
        }

        post.setEliminado(true);
        post.setFechaEliminacion(Instant.now());
        postRepository.save(post);
        log.info("Post {} eliminado exitosamente", postId);
    }
}