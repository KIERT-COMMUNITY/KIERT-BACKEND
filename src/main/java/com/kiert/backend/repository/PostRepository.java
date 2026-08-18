package com.kiert.backend.repository;

import com.kiert.backend.entity.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"autor", "adjuntos"})
    List<Post> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {"autor", "adjuntos"})
    Optional<Post> findById(Long id);

    // ✅ Métodos para filtrar posts eliminados
    @Query("SELECT p FROM Post p WHERE p.eliminado = false ORDER BY p.fechaCreacion DESC")
    List<Post> findAllActiveOrderByFechaCreacionDesc();

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.eliminado = false")
    Optional<Post> findActiveById(@Param("id") Long id);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.autor.id = :autorId AND p.eliminado = false")
    long countActiveByAutorId(@Param("autorId") Long autorId);

    @Query("SELECT p FROM Post p WHERE p.autor.id = :autorId AND p.eliminado = false ORDER BY p.fechaCreacion DESC")
    List<Post> findActiveByAutorIdOrderByFechaCreacionDesc(@Param("autorId") Long autorId);
}