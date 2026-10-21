// src/main/java/com/kiert/backend/mapper/PostMapper.java
package com.kiert.backend.mapper;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.Adjunto;
import com.kiert.backend.entity.Comentario;
import com.kiert.backend.entity.Post;
import com.kiert.backend.entity.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PostMapper {

    // ============================================================
    // MAPEO DE POST
    // ============================================================

    /**
     * Mapeo básico. Usa las colecciones lazy del post.
     * Puede generar N+1 si no están precargadas.
     */
    public PostDTO aDTO(Post post) {
        return aDTO(post, null, null);
    }

    /**
     * Sobrecarga optimizada: permite pasar adjuntos y contador de comentarios
     * precargados en queries separadas, evitando N+1 y el bug HHH000104.
     *
     * @param post             entidad Post (debe tener autor y personalización precargados)
     * @param adjuntosExternos si no es null, se usa en lugar de post.getAdjuntos()
     * @param totalComentarios si no es null, se usa en lugar de contar en post.getComentarios()
     */
    public PostDTO aDTO(Post post, List<Adjunto> adjuntosExternos, Long totalComentarios) {
        if (post == null) return null;

        // Categoría
        String categoria = post.getCategoria() != null ? post.getCategoria() : "otro";

        // Adjuntos: usar los externos si vienen
        List<Adjunto> adjuntos = adjuntosExternos != null
                ? adjuntosExternos
                : (post.getAdjuntos() != null ? post.getAdjuntos() : Collections.emptyList());

        List<AdjuntoDTO> adjuntosDTO = adjuntos.stream()
                .filter(a -> a != null)
                .map(this::aDTO)
                .toList();

        // Comentarios: usar el contador externo si viene
        long comentarios;
        if (totalComentarios != null) {
            comentarios = totalComentarios;
        } else if (post.getComentarios() != null) {
            comentarios = post.getComentarios().stream()
                    .filter(c -> !c.isEliminado())
                    .count();
        } else {
            comentarios = 0;
        }

        return new PostDTO(
                post.getId(),
                aAutorResumen(post.getAutor()),
                post.getTitulo(),
                post.getDescripcion(),
                categoria,
                adjuntosDTO,
                comentarios,
                post.getFechaCreacion()
        );
    }

    // ============================================================
    // MAPEO DE ADJUNTO
    // ============================================================

    public AdjuntoDTO aDTO(Adjunto adjunto) {
        if (adjunto == null) return null;

        return new AdjuntoDTO(
                adjunto.getId(),
                adjunto.getTipo() != null ? adjunto.getTipo() : "archivo",
                adjunto.getNombre() != null ? adjunto.getNombre() : "Sin nombre",
                adjunto.getUrl() != null ? adjunto.getUrl() : "",
                adjunto.getPesoKb() != null ? adjunto.getPesoKb() : 0,
                adjunto.getDuracionSegundos(),
                adjunto.getAncho(),
                adjunto.getAlto(),
                adjunto.getFormato()
        );
    }

    // ============================================================
    // MAPEO DE COMENTARIO
    // ============================================================

    /**
     * Mapeo básico. Cuenta reacciones desde la colección lazy.
     * Puede generar N+1.
     */
    public ComentarioDTO aDTO(Comentario comentario) {
        return aDTO(comentario, null);
    }

    /**
     * Sobrecarga optimizada: permite pasar el conteo de reacciones
     * ya calculado (en lugar de contarlas desde la colección lazy).
     *
     * @param comentario       entidad Comentario
     * @param reaccionesExternas mapa con contadores {like: N, love: N}
     */
    public ComentarioDTO aDTO(Comentario comentario, Map<String, Long> reaccionesExternas) {
        if (comentario == null) return null;

        long likes = 0;
        long loves = 0;

        if (reaccionesExternas != null) {
            likes = reaccionesExternas.getOrDefault("like", 0L);
            loves = reaccionesExternas.getOrDefault("love", 0L);
        } else if (comentario.getReacciones() != null) {
            for (var r : comentario.getReacciones()) {
                if ("like".equals(r.getTipo())) likes++;
                else if ("love".equals(r.getTipo())) loves++;
            }
        }

        return new ComentarioDTO(
                comentario.getId(),
                aAutorResumen(comentario.getAutor()),
                comentario.getContenido(),
                comentario.getFechaCreacion(),
                comentario.getUrlImagen(),
                new ReaccionesDTO(likes, loves, 0, 0, 0, 0)
        );
    }

    // ============================================================
    // MAPEO DE AUTOR
    // ============================================================

    /**
     * Mapea un usuario a un resumen público.
     * Maneja LazyInitializationException de forma segura.
     */
    public AutorResumenDTO aAutorResumen(Usuario usuario) {
        if (usuario == null) return null;

        String marcoId = "none";

        try {
            // Intenta obtener el marco desde la personalización
            if (usuario.getPersonalizacion() != null) {
                marcoId = usuario.getPersonalizacion().getMarcoId();
            }
        } catch (Exception e) {
            // LazyInitializationException u otro error → usar fallback
            log.debug("No se pudo cargar la personalización del usuario {}: {}",
                    usuario.getId(), e.getMessage());
        }

        // Fallback al campo marco_id del usuario
        if ((marcoId == null || marcoId.isEmpty()) && usuario.getMarcoId() != null) {
            marcoId = usuario.getMarcoId();
        }

        if (marcoId == null || marcoId.isEmpty()) {
            marcoId = "none";
        }

        return new AutorResumenDTO(
                usuario.getId(),
                usuario.getNombreUsuario() != null ? usuario.getNombreUsuario() : "Usuario",
                usuario.getFotoPerfilUrl(),
                marcoId
        );
    }
}