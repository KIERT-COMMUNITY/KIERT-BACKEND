package com.kiert.backend.repository;

import com.kiert.backend.entity.Compartido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompartidoRepository extends JpaRepository<Compartido, Long> {

    @Query("SELECT c FROM Compartido c " +
            "LEFT JOIN FETCH c.usuario " +
            "LEFT JOIN FETCH c.post " +
            "WHERE c.post.id = :postId " +
            "ORDER BY c.fechaCreacion DESC")
    List<Compartido> findByPostId(@Param("postId") Long postId);

    long countByPostId(Long postId);

    boolean existsByUsuarioIdAndPostId(Long usuarioId, Long postId);
}