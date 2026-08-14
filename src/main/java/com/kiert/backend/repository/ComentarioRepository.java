package com.kiert.backend.repository;

import com.kiert.backend.entity.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findByPostIdOrderByFechaCreacionAsc(Long postId);
}