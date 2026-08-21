package com.kiert.backend.repository;

import com.kiert.backend.entity.Reaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReaccionRepository extends JpaRepository<Reaccion, Long> {

    Optional<Reaccion> findByUsuarioIdAndPostId(Long usuarioId, Long postId);

    Optional<Reaccion> findByUsuarioIdAndComentarioId(Long usuarioId, Long comentarioId);

    @Query("SELECT r.tipo, COUNT(r) FROM Reaccion r WHERE r.post.id = :postId GROUP BY r.tipo")
    List<Object[]> countReaccionesByPost(@Param("postId") Long postId);

    @Query("SELECT r.tipo, COUNT(r) FROM Reaccion r WHERE r.comentario.id = :comentarioId GROUP BY r.tipo")
    List<Object[]> countReaccionesByComentario(@Param("comentarioId") Long comentarioId);

    long countByPostId(Long postId);

    long countByComentarioId(Long comentarioId);

    void deleteByUsuarioIdAndPostId(Long usuarioId, Long postId);

    void deleteByUsuarioIdAndComentarioId(Long usuarioId, Long comentarioId);
}