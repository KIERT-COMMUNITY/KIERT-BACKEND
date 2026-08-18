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

    // ========== LISTAR POSTS (SOLO ACTIVOS) ==========
    @Transactional(readOnly = true)
    public List<PostDTO> listar() {
        log.info("📋 Listando posts desde BD");
        try {
            List<Post> posts = postRepository.findAllActiveOrderByFechaCreacionDesc();
            log.info("✅ Se encontraron {} posts", posts.size());
            return posts.stream()
                    .map(postMapper::aDTO)
                    .toList();
        } catch (Exception e) {
            log.error("❌ Error al listar posts: {}", e.getMessage(), e);
            throw new RuntimeException("Error al listar publicaciones: " + e.getMessage());
        }
    }

    // ========== OBTENER POST POR ID (SOLO ACTIVOS) ==========
    @Transactional(readOnly = true)
    public PostDTO obtenerPorId(Long id) {
        log.info("🔍 Obteniendo post {} desde BD", id);
        Post post = postRepository.findActiveById(id)
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
                .eliminado(false)
                .fechaCreacion(Instant.now())
                .build();

        // Guardar link si existe
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

        // Guardar el post primero
        post = postRepository.save(post);
        log.info("✅ Post guardado con ID: {}", post.getId());

        // Subir archivos a Cloudinary
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

    // ========== ELIMINAR POST (SOFT DELETE) ==========
    @Transactional
    public void eliminarPost(Long postId, Long autorId) {
        log.info("🗑️ Eliminando post {} por usuario {}", postId, autorId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación."));

        // Verificar que el usuario sea el autor
        if (!post.getAutor().getId().equals(autorId)) {
            throw new SecurityException("No tienes permiso para eliminar esta publicación.");
        }

        // Soft delete
        post.setEliminado(true);
        post.setFechaEliminacion(Instant.now());
        postRepository.save(post);
        log.info("✅ Post {} eliminado exitosamente", postId);
    }

    // ========== ACTUALIZAR POST ==========
    @Transactional
    public PostDTO actualizarPost(Long postId, Long autorId, ActualizarPostDTO datos) {
        log.info("✏️ Actualizando post {} por usuario {}", postId, autorId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la publicación."));

        if (!post.getAutor().getId().equals(autorId)) {
            throw new SecurityException("No tienes permiso para actualizar esta publicación.");
        }

        if (datos.titulo() != null && !datos.titulo().isBlank()) {
            post.setTitulo(datos.titulo());
        }
        if (datos.descripcion() != null && !datos.descripcion().isBlank()) {
            post.setDescripcion(datos.descripcion());
        }
        if (datos.categoria() != null && !datos.categoria().isBlank()) {
            post.setCategoria(CategoriaPost.desdeValor(datos.categoria()));
        }

        post.setFechaActualizacion(Instant.now());
        post = postRepository.save(post);
        return postMapper.aDTO(post);
    }

    // ========== LIMPIAR CACHÉ DE POSTS ==========
    public void limpiarCachePosts() {
        log.info("🧹 Limpiando caché de posts");
    }
}