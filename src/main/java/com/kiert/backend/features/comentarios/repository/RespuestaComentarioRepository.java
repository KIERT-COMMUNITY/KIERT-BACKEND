// src/main/java/com/kiert/backend/repository/RespuestaComentarioRepository.java
package com.kiert.backend.features.comentarios.repository;

import com.kiert.backend.entity.RespuestaComentario;
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
public interface RespuestaComentarioRepository extends JpaRepository<RespuestaComentario, Long> {

    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.autor
            WHERE r.comentario.id = :comentarioId
              AND r.eliminado = false
            ORDER BY r.fechaCreacion ASC
            """)
    List<RespuestaComentario> findByComentarioIdAndEliminadoFalseOrderByFechaCreacionAsc(
            @Param("comentarioId") Long comentarioId
    );

    default List<RespuestaComentario> findActiveByComentarioId(Long comentarioId) {
        return findByComentarioIdAndEliminadoFalseOrderByFechaCreacionAsc(comentarioId);
    }

    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.autor
            WHERE r.comentario.id = :comentarioId
              AND r.eliminado = false
            ORDER BY r.fechaCreacion ASC
            """)
    Page<RespuestaComentario> findByComentarioIdPaginado(
            @Param("comentarioId") Long comentarioId,
            Pageable pageable
    );

    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.autor
            WHERE r.comentario.id IN :comentarioIds
              AND r.eliminado = false
            ORDER BY r.comentario.id ASC, r.fechaCreacion ASC
            """)
    List<RespuestaComentario> findByComentarioIdIn(
            @Param("comentarioIds") List<Long> comentarioIds
    );

    @Query("""
            SELECT COUNT(r) FROM RespuestaComentario r
            WHERE r.comentario.id = :comentarioId
              AND r.eliminado = false
            """)
    long countActiveByComentarioId(@Param("comentarioId") Long comentarioId);

    @Query("""
            SELECT r.comentario.id, COUNT(r)
            FROM RespuestaComentario r
            WHERE r.comentario.id IN :comentarioIds
              AND r.eliminado = false
            GROUP BY r.comentario.id
            """)
    List<Object[]> contarPorComentarios(@Param("comentarioIds") List<Long> comentarioIds);

    @Query("""
            SELECT COUNT(r) FROM RespuestaComentario r
            WHERE r.autor.id = :usuarioId
              AND r.eliminado = false
            """)
    long countActiveByAutorId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.comentario c
            LEFT JOIN FETCH c.post
            WHERE r.autor.id = :usuarioId
              AND r.eliminado = false
              AND c.eliminado = false
            ORDER BY r.fechaCreacion DESC
            """)
    Page<RespuestaComentario> findUltimasDeUsuario(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.autor
            LEFT JOIN FETCH r.comentario c
            LEFT JOIN FETCH c.post
            WHERE r.id = :id
              AND r.eliminado = false
            """)
    Optional<RespuestaComentario> findActivaById(@Param("id") Long id);

    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.autor
            WHERE r.comentario.id = :comentarioId
              AND r.eliminado = false
              AND LOWER(r.contenido) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY r.fechaCreacion DESC
            """)
    Page<RespuestaComentario> buscarEnComentario(
            @Param("comentarioId") Long comentarioId,
            @Param("query") String query,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE RespuestaComentario r
            SET r.eliminado = true,
                r.fechaEliminacion = :fecha
            WHERE r.comentario.id = :comentarioId
              AND r.eliminado = false
            """)
    int eliminarPorComentario(
            @Param("comentarioId") Long comentarioId,
            @Param("fecha") Instant fecha
    );

    @Modifying
    @Query("""
            UPDATE RespuestaComentario r
            SET r.eliminado = true,
                r.fechaEliminacion = :fecha
            WHERE r.comentario.id IN :comentarioIds
              AND r.eliminado = false
            """)
    int eliminarPorComentarios(
            @Param("comentarioIds") List<Long> comentarioIds,
            @Param("fecha") Instant fecha
    );

    @Modifying
    @Query(value = """
            DELETE FROM respuestas_comentarios
            WHERE eliminado = true
              AND fecha_eliminacion < :limite
            """, nativeQuery = true)
    int eliminarRespuestasBorradasAntiguas(@Param("limite") Instant limite);

    @Query("SELECT COUNT(r) FROM RespuestaComentario r WHERE r.eliminado = false")
    long countActivas();

    @Query("""
            SELECT r.comentario.id, COUNT(r) as total
            FROM RespuestaComentario r
            WHERE r.eliminado = false
            GROUP BY r.comentario.id
            ORDER BY total DESC
            """)
    List<Object[]> topComentariosConMasRespuestas(Pageable pageable);

    // ============================================================
    // SEGURIDAD: Validar autoría
    // ============================================================

    /**
     * NUEVO: Verifica si un usuario es el autor de la respuesta.
     * Se usa para prevenir que un usuario edite/elimine respuestas ajenas.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM RespuestaComentario r
            WHERE r.id = :respuestaId
              AND r.autor.id = :usuarioId
              AND r.eliminado = false
            """)
    boolean esAutorDeRespuesta(
            @Param("respuestaId") Long respuestaId,
            @Param("usuarioId") Long usuarioId
    );
}