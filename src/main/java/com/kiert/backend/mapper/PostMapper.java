// src/main/java/com/kiert/backend/mapper/PostMapper.java
package com.kiert.backend.mapper;

import com.kiert.backend.entity.Adjunto;
import com.kiert.backend.entity.Comentario;
import com.kiert.backend.entity.Post;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.features.autor.dto.AutorResumenDTO;
import com.kiert.backend.features.bloqueo.dto.AdjuntoDTO;
import com.kiert.backend.features.comentarios.dto.ComentarioDTO;
import com.kiert.backend.features.post.dto.PostDTO;
import com.kiert.backend.features.reacciones.dto.ReaccionesDTO;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PostMapper {

    public PostDTO aDTO(Post post) {
        return aDTO(post, null, null);
    }

    public PostDTO aDTO(Post post, List<Adjunto> adjuntosExternos, Long totalComentarios) {
        if (post == null) return null;

        String categoria = post.getCategoria() != null ? post.getCategoria() : "otro";

        List<Adjunto> adjuntos = adjuntosExternos != null
                ? adjuntosExternos
                : (post.getAdjuntos() != null ? post.getAdjuntos() : Collections.emptyList());

        List<AdjuntoDTO> adjuntosDTO = adjuntos.stream()
                .filter(a -> a != null)
                .map(this::aDTO)
                .toList();

        long comentarios;
        if (totalComentarios != null) {
            comentarios = totalComentarios;
        } else if (Hibernate.isInitialized(post.getComentarios()) && post.getComentarios() != null) {
            comentarios = post.getComentarios().stream()
                    .filter(c -> !c.isEliminado())
                    .count();
        } else {
            // Si la coleccion no esta inicializada, no forzamos carga (evita N+1 y LazyInitializationException)
            comentarios = 0;
        }

        boolean esAnonimo = Boolean.TRUE.equals(post.getEsAnonimo());

        AutorResumenDTO autorDTO = esAnonimo
                ? autorAnonimo()
                : aAutorResumen(post.getAutor());

        return new PostDTO(
                post.getId(),
                autorDTO,
                post.getTitulo(),
                post.getDescripcion(),
                categoria,
                adjuntosDTO,
                comentarios,
                post.getFechaCreacion(),
                esAnonimo
        );
    }

    private AutorResumenDTO autorAnonimo() {
        return new AutorResumenDTO(
                null,
                "Anonimo",
                null,
                "none"
        );
    }

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

    public ComentarioDTO aDTO(Comentario comentario) {
        return aDTO(comentario, null);
    }

    public ComentarioDTO aDTO(Comentario comentario, Map<String, Long> reaccionesExternas) {
        if (comentario == null) return null;

        long likes = 0;
        long loves = 0;

        if (reaccionesExternas != null) {
            // Acepta tanto "likes"/"loves" (plural, como los devuelve el servicio)
            // como "like"/"love" (singular, por compatibilidad)
            likes = reaccionesExternas.getOrDefault("likes",
                    reaccionesExternas.getOrDefault("like", 0L));
            loves = reaccionesExternas.getOrDefault("loves",
                    reaccionesExternas.getOrDefault("love", 0L));
        } else if (Hibernate.isInitialized(comentario.getReacciones()) && comentario.getReacciones() != null) {
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

    public AutorResumenDTO aAutorResumen(Usuario usuario) {
        if (usuario == null) return null;

        String marcoId = "none";

        try {
            if (Hibernate.isInitialized(usuario.getPersonalizacion())
                    && usuario.getPersonalizacion() != null) {
                marcoId = usuario.getPersonalizacion().getMarcoId();
            }
        } catch (Exception e) {
            log.debug("No se pudo cargar personalizacion del usuario {}: {}",
                    usuario.getId(), e.getMessage());
        }

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