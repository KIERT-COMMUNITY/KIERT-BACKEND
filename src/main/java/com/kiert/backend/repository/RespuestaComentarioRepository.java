package com.kiert.backend.repository;

import com.kiert.backend.entity.RespuestaComentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RespuestaComentarioRepository extends JpaRepository<RespuestaComentario, Long> {

    List<RespuestaComentario> findByComentarioIdAndEliminadoFalseOrderByFechaCreacionAsc(Long comentarioId);

    @Query("SELECT r FROM RespuestaComentario r WHERE r.comentario.id = :comentarioId AND r.eliminado = false ORDER BY r.fechaCreacion ASC")
    List<RespuestaComentario> findActiveByComentarioId(@Param("comentarioId") Long comentarioId);

    @Query("SELECT COUNT(r) FROM RespuestaComentario r WHERE r.comentario.id = :comentarioId AND r.eliminado = false")
    long countActiveByComentarioId(@Param("comentarioId") Long comentarioId);
}