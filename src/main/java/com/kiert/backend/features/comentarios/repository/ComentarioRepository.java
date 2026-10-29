// src/main/java/com/kiert/backend/features/comentarios/repository/ComentarioRepository.java
package com.kiert.backend.features.comentarios.repository;

import com.kiert.backend.entity.Comentario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.autor
            WHERE c.post.id = :postId
              AND c.eliminado = false
            ORDER BY c.fechaCreacion ASC
            """)
    List<Comentario> findByPostIdAndEliminadoFalseOrderByFechaCreacionAsc(
            @Param("postId") Long postId
    );

    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.autor
            WHERE c.post.id = :postId
              AND c.eliminado = false
            ORDER BY c.fechaCreacion ASC
            """)
    Page<Comentario> findByPostIdPaginado(
            @Param("postId") Long postId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(c) FROM Comentario c
            WHERE c.post.id = :postId
              AND c.eliminado = false
            """)
    long countActiveByPostId(@Param("postId") Long postId);

    @Query("""
            SELECT COUNT(c) FROM Comentario c
            WHERE c.autor.id = :usuarioId
              AND c.eliminado = false
            """)
    long countActiveByAutorId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.autor
            WHERE c.post.id = :postId
              AND c.eliminado = false
            ORDER BY c.fechaCreacion ASC
            """)
    List<Comentario> findComentariosDePost(@Param("postId") Long postId);

    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.post p
            WHERE c.autor.id = :usuarioId
              AND c.eliminado = false
              AND p.eliminado = false
            ORDER BY c.fechaCreacion DESC
            """)
    Page<Comentario> findUltimosComentariosDeUsuario(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.autor
            LEFT JOIN FETCH c.post
            WHERE c.post.id = :postId
              AND c.eliminado = false
              AND LOWER(c.contenido) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY c.fechaCreacion DESC
            """)
    Page<Comentario> buscarEnComentarios(
            @Param("postId") Long postId,
            @Param("query") String query,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE Comentario c
            SET c.eliminado = true,
                c.fechaEliminacion = :fecha
            WHERE c.post.id = :postId
              AND c.eliminado = false
            """)
    int eliminarPorPostId(
            @Param("postId") Long postId,
            @Param("fecha") Instant fecha
    );

    @Query("""
            SELECT c.post.id, COUNT(c)
            FROM Comentario c
            WHERE c.post.id IN :postIds
              AND c.eliminado = false
            GROUP BY c.post.id
            """)
    List<Object[]> contarComentariosPorPosts(@Param("postIds") List<Long> postIds);

    @Query("""
            SELECT COUNT(c) > 0 FROM Comentario c
            WHERE c.id = :id
              AND c.eliminado = false
            """)
    boolean existeActivo(@Param("id") Long id);

    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.autor
            LEFT JOIN FETCH c.post
            WHERE c.id = :id
              AND c.eliminado = false
            """)
    Optional<Comentario> findActivoConAutor(@Param("id") Long id);

    @Query("""
            SELECT COUNT(c) > 0 FROM Comentario c
            WHERE c.id = :comentarioId
              AND c.autor.id = :usuarioId
              AND c.eliminado = false
            """)
    boolean esAutorDelComentario(
            @Param("comentarioId") Long comentarioId,
            @Param("usuarioId") Long usuarioId
    );
}