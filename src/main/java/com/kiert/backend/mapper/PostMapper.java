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

    public PostDTO aDTO(Post post) {
        if (post == null) {
            return null;
        }

        try {
            // ✅ OBTENER LA CATEGORÍA COMO STRING
            String categoria = post.getCategoria();
            if (categoria == null) {
                categoria = "otro";
            }

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
                    categoria,
                    adjuntos,
                    (int) totalComentarios,
                    post.getFechaCreacion()
            );
        } catch (Exception e) {
            // ✅ LOG DEL ERROR PARA DEPURACIÓN
            System.err.println("❌ Error en PostMapper.aDTO para post ID: " + post.getId());
            e.printStackTrace();
            throw new RuntimeException("Error al mapear post: " + e.getMessage());
        }
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
        if (comentario == null) return null;

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

    public AutorResumenDTO aAutorResumen(Usuario usuario) {
        if (usuario == null) return null;

        try {
            // ✅ OBTENER EL MARCO DEL USUARIO DE MANERA SEGURA
            String marcoId = null;

            // ✅ Verificar si tiene personalización y obtener el marco
            if (usuario.getPersonalizacion() != null) {
                marcoId = usuario.getPersonalizacion().getMarcoId();
            }

            // ✅ Si no tiene marco, usar "none"
            if (marcoId == null || marcoId.isEmpty()) {
                marcoId = "none";
            }

            return new AutorResumenDTO(
                    usuario.getId(),
                    usuario.getNombreUsuario() != null ? usuario.getNombreUsuario() : "Usuario",
                    usuario.getFotoPerfilUrl(),
                    marcoId
            );
        } catch (Exception e) {
            System.err.println("❌ Error al obtener marco del usuario ID: " + usuario.getId());
            e.printStackTrace();
            // ✅ Si hay error, devolver con marco "none"
            return new AutorResumenDTO(
                    usuario.getId(),
                    usuario.getNombreUsuario() != null ? usuario.getNombreUsuario() : "Usuario",
                    usuario.getFotoPerfilUrl(),
                    "none"
            );
        }
    }
}