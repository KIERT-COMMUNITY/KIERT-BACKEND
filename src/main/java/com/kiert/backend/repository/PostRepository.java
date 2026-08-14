package com.kiert.backend.repository;

import com.kiert.backend.entity.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"autor", "adjuntos"})
    List<Post> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {"autor", "adjuntos"})
    Optional<Post> findById(Long id);
}