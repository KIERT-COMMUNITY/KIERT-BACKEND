package com.kiert.backend.repository;

import com.kiert.backend.entity.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    // Método para listar comentarios de un post ordenados por fecha
    List<Comentario> findByPostIdOrderByFechaCreacionAsc(Long postId);

    // Método para listar comentarios activos (no eliminados)
    List<Comentario> findByPostIdAndEliminadoFalseOrderByFechaCreacionAsc(Long postId);

    // Método para contar comentarios de un post
    @Query("SELECT COUNT(c) FROM Comentario c WHERE c.post.id = :postId AND c.eliminado = false")
    long countActiveByPostId(@Param("postId") Long postId);

    // Método para listar comentarios con respuestas
    @Query("SELECT c FROM Comentario c LEFT JOIN FETCH c.respuestas WHERE c.post.id = :postId AND c.eliminado = false ORDER BY c.fechaCreacion ASC")
    List<Comentario> findActiveWithRespuestasByPostId(@Param("postId") Long postId);
}