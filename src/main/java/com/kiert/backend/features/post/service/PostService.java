// src/main/java/com/kiert/backend/features/post/service/PostService.java
package com.kiert.backend.features.post.service;

import com.kiert.backend.entity.*;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.comentarios.dto.ComentarioDTO;
import com.kiert.backend.features.post.dto.CrearPostDTO;
import com.kiert.backend.features.post.dto.PostDTO;
import com.kiert.backend.mapper.PostMapper;
import com.kiert.backend.features.comentarios.repository.ComentarioRepository;
import com.kiert.backend.features.post.repository.PostRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import com.kiert.backend.features.cloudinary.service.CloudinaryService;
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

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_POSTS, key = "'all'")
    public List<PostDTO> listar() {
        log.info("[DB] Listando posts");
        List<Post> posts = postRepository.findAllActiveOrderByFechaCreacionDesc();
        return posts.stream()
                .map(postMapper::aDTO)
                .toList();
    }

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

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_POSTS, allEntries = true),
            @CacheEvict(value = CACHE_POST, allEntries = true)
    })
    public PostDTO crear(Long autorId, CrearPostDTO datos, List<MultipartFile> archivos) {
        log.info("Creando post para usuario {}", autorId);

        if (autorId == null) {
            throw new BadRequestException("Usuario requerido");
        }
        if (datos == null) {
            throw new BadRequestException("Datos requeridos");
        }
        if (datos.titulo() == null || datos.titulo().isBlank()) {
            throw new BadRequestException("El titulo es obligatorio");
        }
        if (datos.titulo().length() > MAX_TITULO) {
            throw new BadRequestException("El titulo es demasiado largo");
        }
        if (datos.descripcion() != null && datos.descripcion().length() > MAX_DESCRIPCION) {
            throw new BadRequestException("La descripcion es demasiado larga");
        }
        if (archivos != null && archivos.size() > MAX_ARCHIVOS) {
            throw new BadRequestException("Solo se permiten " + MAX_ARCHIVOS + " archivos");
        }

        List<MultipartFile> archivosValidos = (archivos != null)
                ? archivos.stream()
                  .filter(a -> a != null && !a.isEmpty() && a.getSize() > 0)
                  .toList()
                : List.of();

        long totalBytes = archivosValidos.stream().mapToLong(MultipartFile::getSize).sum();
        if (totalBytes > MAX_TOTAL_ARCHIVOS) {
            throw new BadRequestException("Los archivos no pueden superar 30 MB en total");
        }

        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String categoriaUsuario = datos.categoria() != null ? datos.categoria().trim() : "otro";
        boolean esAnonimo = Boolean.TRUE.equals(datos.esAnonimo());

        Post post = Post.builder()
                .autor(autor)
                .titulo(datos.titulo().trim())
                .categoria(categoriaUsuario)
                .descripcion(datos.descripcion())
                .eliminado(false)
                .esAnonimo(esAnonimo)
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

        int subidosOk = 0;
        int fallidos = 0;

        if (!archivosValidos.isEmpty()) {
            log.info("Subiendo {} archivo(s) a Cloudinary", archivosValidos.size());

            for (MultipartFile archivo : archivosValidos) {
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

                    if (url == null || url.isBlank()) {
                        throw new RuntimeException("Cloudinary devolvio URL vacia");
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
                    subidosOk++;

                } catch (Exception e) {
                    fallidos++;
                    log.error("Error al subir '{}': {}",
                            archivo.getOriginalFilename(), e.getMessage());

                    if (subidosOk == 0 && fallidos == archivosValidos.size()) {
                        throw new BadRequestException(
                                "No se pudo subir ningun archivo. Intenta de nuevo.");
                    }
                }
            }
        }

        // Guardar y asegurar que el ID esté disponible
        post = postRepository.save(post);
        postRepository.flush();

        if (post.getId() == null) {
            throw new RuntimeException("Error interno: el post no obtuvo ID");
        }

        log.info("Post creado: id={}, anonimo={}, adjuntos={}",
                post.getId(), esAnonimo, post.getAdjuntos().size());

        return postMapper.aDTO(post);
    }

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

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_COMMENTS, key = "#postId"),
            @CacheEvict(value = CACHE_POST, key = "#postId"),
            @CacheEvict(value = CACHE_POSTS, allEntries = true)
    })
    public ComentarioDTO comentar(Long postId, Long autorId, String contenido) {
        log.info("Comentando en post {} por usuario {}", postId, autorId);

        if (contenido == null || contenido.isBlank()) {
            throw new BadRequestException("El comentario no puede estar vacio");
        }
        if (contenido.length() > MAX_COMENTARIO) {
            throw new BadRequestException("El comentario es demasiado largo");
        }

        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación"));

        if (post.isEliminado()) {
            throw new BadRequestException("La publicacion ya no esta disponible");
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

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_POSTS, allEntries = true),
            @CacheEvict(value = CACHE_POST, key = "#postId"),
            @CacheEvict(value = CACHE_COMMENTS, key = "#postId")
    })
    public void eliminarPost(Long postId, Long autorId) {
        log.info("Eliminando post {} por usuario {}", postId, autorId);

        if (postId == null || autorId == null) {
            throw new BadRequestException("Parametros invalidos");
        }

        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación"));

        if (!post.getAutor().getId().equals(autorId)) {
            throw new SecurityException("No tienes permiso para eliminar esta publicacion");
        }

        post.setEliminado(true);
        post.setFechaEliminacion(Instant.now());
        postRepository.save(post);
    }
}