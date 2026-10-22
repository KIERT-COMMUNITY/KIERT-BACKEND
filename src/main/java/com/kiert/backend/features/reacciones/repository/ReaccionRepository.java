// src/main/java/com/kiert/backend/repository/ReaccionRepository.java
package com.kiert.backend.features.reacciones.repository;

import com.kiert.backend.entity.Reaccion;
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
public interface ReaccionRepository extends JpaRepository<Reaccion, Long> {

    // ============================================================
    // BUSCAR REACCIÓN ESPECÍFICA
    // ============================================================

    @Query("""
            SELECT r FROM Reaccion r
            LEFT JOIN FETCH r.usuario
            WHERE r.usuario.id = :usuarioId
              AND r.post.id = :postId
            """)
    Optional<Reaccion> findByUsuarioIdAndPostId(
            @Param("usuarioId") Long usuarioId,
            @Param("postId") Long postId
    );

    @Query("""
            SELECT r FROM Reaccion r
            LEFT JOIN FETCH r.usuario
            WHERE r.usuario.id = :usuarioId
              AND r.comentario.id = :comentarioId
            """)
    Optional<Reaccion> findByUsuarioIdAndComentarioId(
            @Param("usuarioId") Long usuarioId,
            @Param("comentarioId") Long comentarioId
    );

    // ============================================================
    // CONTAR REACCIONES (1 post / 1 comentario)
    // ============================================================

    @Query("""
            SELECT r.tipo, COUNT(r)
            FROM Reaccion r
            WHERE r.post.id = :postId
            GROUP BY r.tipo
            """)
    List<Object[]> countReaccionesByPost(@Param("postId") Long postId);

    @Query("""
            SELECT r.tipo, COUNT(r)
            FROM Reaccion r
            WHERE r.comentario.id = :comentarioId
            GROUP BY r.tipo
            """)
    List<Object[]> countReaccionesByComentario(@Param("comentarioId") Long comentarioId);

    long countByPostId(Long postId);

    long countByComentarioId(Long comentarioId);

    // ============================================================
    // CONTAR REACCIONES POR MÚLTIPLES POSTS / COMENTARIOS
    // ============================================================

    @Query("""
            SELECT r.post.id, r.tipo, COUNT(r)
            FROM Reaccion r
            WHERE r.post.id IN :postIds
            GROUP BY r.post.id, r.tipo
            """)
    List<Object[]> contarReaccionesPorPosts(@Param("postIds") List<Long> postIds);

    @Query("""
            SELECT r.comentario.id, r.tipo, COUNT(r)
            FROM Reaccion r
            WHERE r.comentario.id IN :comentarioIds
            GROUP BY r.comentario.id, r.tipo
            """)
    List<Object[]> contarReaccionesPorComentarios(
            @Param("comentarioIds") List<Long> comentarioIds
    );

    // ============================================================
    // REACCIONES DEL USUARIO
    // ============================================================

    @Query("""
            SELECT r FROM Reaccion r
            LEFT JOIN FETCH r.post p
            WHERE r.usuario.id = :usuarioId
              AND r.post IS NOT NULL
              AND p.eliminado = false
            ORDER BY r.fechaCreacion DESC
            """)
    Page<Reaccion> findReaccionesAPostsDeUsuario(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Query("""
            SELECT r FROM Reaccion r
            LEFT JOIN FETCH r.comentario c
            WHERE r.usuario.id = :usuarioId
              AND r.comentario IS NOT NULL
              AND c.eliminado = false
            ORDER BY r.fechaCreacion DESC
            """)
    Page<Reaccion> findReaccionesAComentariosDeUsuario(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    // ============================================================
    // VERIFICAR REACCIONES (bulk, para el feed)
    // ============================================================

    @Query("""
            SELECT r.post.id FROM Reaccion r
            WHERE r.usuario.id = :usuarioId
              AND r.post.id IN :postIds
            """)
    List<Long> findPostsDondeReaccione(
            @Param("usuarioId") Long usuarioId,
            @Param("postIds") List<Long> postIds
    );

    @Query("""
            SELECT r.comentario.id FROM Reaccion r
            WHERE r.usuario.id = :usuarioId
              AND r.comentario.id IN :comentarioIds
            """)
    List<Long> findComentariosDondeReaccione(
            @Param("usuarioId") Long usuarioId,
            @Param("comentarioIds") List<Long> comentarioIds
    );

    // ============================================================
    // ELIMINAR REACCIONES
    // ============================================================

    @Modifying
    @Query("""
            DELETE FROM Reaccion r
            WHERE r.usuario.id = :usuarioId
              AND r.post.id = :postId
            """)
    int deleteByUsuarioIdAndPostId(
            @Param("usuarioId") Long usuarioId,
            @Param("postId") Long postId
    );

    @Modifying
    @Query("""
            DELETE FROM Reaccion r
            WHERE r.usuario.id = :usuarioId
              AND r.comentario.id = :comentarioId
            """)
    int deleteByUsuarioIdAndComentarioId(
            @Param("usuarioId") Long usuarioId,
            @Param("comentarioId") Long comentarioId
    );

    @Modifying
    @Query("DELETE FROM Reaccion r WHERE r.post.id = :postId")
    int deleteByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("DELETE FROM Reaccion r WHERE r.comentario.id = :comentarioId")
    int deleteByComentarioId(@Param("comentarioId") Long comentarioId);

    // ============================================================
    // TOP POSTS REACCIONADOS
    // ============================================================

    @Query("""
            SELECT r.post.id, COUNT(r) as total
            FROM Reaccion r
            WHERE r.post IS NOT NULL
            GROUP BY r.post.id
            ORDER BY total DESC
            """)
    List<Object[]> topPostsMasReaccionados(Pageable pageable);

    // ============================================================
    // TIPOS DE REACCIÓN DISPONIBLES
    // ============================================================

    @Query("""
            SELECT DISTINCT r.tipo FROM Reaccion r
            ORDER BY r.tipo ASC
            """)
    List<String> findDistinctTipos();

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    @Query("SELECT COUNT(r) FROM Reaccion r")
    long countTotal();

    @Query("""
            SELECT r.tipo, COUNT(r)
            FROM Reaccion r
            GROUP BY r.tipo
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> contarGlobalPorTipo();

    @Query("""
            SELECT COUNT(r) FROM Reaccion r
            WHERE r.fechaCreacion >= :desde
              AND r.fechaCreacion < :hasta
            """)
    long countEnRango(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );

    // ============================================================
    // LIMPIEZA
    // ============================================================

    @Modifying
    @Query("""
            DELETE FROM Reaccion r
            WHERE r.post IS NULL
              AND r.comentario IS NULL
            """)
    int eliminarHuerfanas();

    // ============================================================
    // SEGURIDAD: Validar autoría de la reacción
    // ============================================================

    /**
     * NUEVO: Verifica si un usuario es el autor de la reacción.
     * Se usa para impedir que un usuario elimine reacciones ajenas.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reaccion r
            WHERE r.id = :reaccionId
              AND r.usuario.id = :usuarioId
            """)
    boolean esReaccionDelUsuario(
            @Param("reaccionId") Long reaccionId,
            @Param("usuarioId") Long usuarioId
    );
}