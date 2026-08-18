package com.kiert.backend.mapper;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.Adjunto;
import com.kiert.backend.entity.Comentario;
import com.kiert.backend.entity.Post;
import com.kiert.backend.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostMapper {

    // ========== POST A DTO ==========
    public PostDTO aDTO(Post post) {
        List<AdjuntoDTO> adjuntos = post.getAdjuntos().stream()
                .filter(adjunto -> adjunto != null)
                .map(this::aDTO)
                .toList();

        long totalComentarios = post.getComentarios() != null
                ? post.getComentarios().stream().filter(c -> !c.isEliminado()).count()
                : 0;

        return new PostDTO(
                post.getId(),
                aAutorResumen(post.getAutor()),
                post.getTitulo(),
                post.getDescripcion(),
                post.getCategoria().getValor(),
                adjuntos,  // ✅ Asegurar que los adjuntos se pasan correctamente
                (int) totalComentarios,
                post.getFechaCreacion()
        );
    }

    // ========== ADJUNTO A DTO ==========
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

    // ========== COMENTARIO A DTO ==========
    public ComentarioDTO aDTO(Comentario comentario) {
        long likes = 0;
        long loves = 0;
        if (comentario.getReacciones() != null) {
            likes = comentario.getReacciones().stream()
                    .filter(r -> "like".equals(r.getTipo()))
                    .count();
            loves = comentario.getReacciones().stream()
                    .filter(r -> "love".equals(r.getTipo()))
                    .count();
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

    // ========== AUTOR A RESUMEN ==========
    public AutorResumenDTO aAutorResumen(Usuario usuario) {
        if (usuario == null) return null;
        return new AutorResumenDTO(
                usuario.getId(),
                usuario.getNombreUsuario() != null ? usuario.getNombreUsuario() : "Usuario",
                usuario.getFotoPerfilUrl()
        );
    }
}