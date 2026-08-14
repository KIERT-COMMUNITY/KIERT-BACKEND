package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.Adjunto;
import com.kiert.backend.entity.Comentario;
import com.kiert.backend.entity.Post;
import com.kiert.backend.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostMapper {

    public PostDTO aDTO(Post post) {
        List<AdjuntoDTO> adjuntos = post.getAdjuntos().stream().map(this::aDTO).toList();
        return new PostDTO(
                post.getId(),
                aAutorResumen(post.getAutor()),
                post.getTitulo(),
                post.getDescripcion(),
                post.getCategoria().getValor(),
                adjuntos,
                post.getComentarios().size(),
                post.getFechaCreacion()
        );
    }

    public AdjuntoDTO aDTO(Adjunto adjunto) {
        return new AdjuntoDTO(
                adjunto.getId(),
                adjunto.getTipo().getValor(),
                adjunto.getNombre(),
                adjunto.getUrl(),
                adjunto.getPesoKb()
        );
    }

    public ComentarioDTO aDTO(Comentario comentario) {
        return new ComentarioDTO(
                comentario.getId(),
                aAutorResumen(comentario.getAutor()),
                comentario.getContenido(),
                comentario.getFechaCreacion()
        );
    }

    private AutorResumenDTO aAutorResumen(Usuario usuario) {
        return new AutorResumenDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getFotoPerfilUrl()
        );
    }
}