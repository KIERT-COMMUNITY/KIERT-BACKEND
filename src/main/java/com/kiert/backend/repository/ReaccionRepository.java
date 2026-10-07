// src/main/java/com/kiert/backend/repository/ReaccionRepository.java
package com.kiert.backend.repository;

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

    /**
     * Busca la reacción de un usuario a un post.
     */
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

    /**
     * Busca la reacción de un usuario a un comentario.
     */
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
    // CONTAR REACCIONES (para el feed — 1 post)
    // ============================================================

    /**
     * Cuenta reacciones agrupadas por tipo en un post.
     * Devuelve [tipo, count].
     */
    @Query("""
            SELECT r.tipo, COUNT(r)
            FROM Reaccion r
            WHERE r.post.id = :postId
            GROUP BY r.tipo
            """)
    List<Object[]> countReaccionesByPost(@Param("postId") Long postId);

    /**
     * Cuenta reacciones agrupadas por tipo en un comentario.
     */
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
    //  OPTIMIZACIÓN CRÍTICA: CONTAR REACCIONES POR MÚLTIPLES POSTS
    // ============================================================

    /**
     * NUEVO: Cuenta reacciones de VARIOS posts en 1 query.
     * Devuelve [postId, tipo, count].
     *
     * Elimina el N+1 del feed: en lugar de 20 queries, es 1.
     *
     * Uso en el service:
     *   Map<Long, Map<String, Long>> contadores = repo
     *       .contarReaccionesPorPosts(postIds)
     *       .stream()
     *       .collect(Collectors.groupingBy(
     *           row -> (Long) row[0],
     *           Collectors.toMap(
     *               row -> (String) row[1],
     *               row -> (Long) row[2]
     *           )
     *       ));
     */
    @Query("""
            SELECT r.post.id, r.tipo, COUNT(r)
            FROM Reaccion r
            WHERE r.post.id IN :postIds
            GROUP BY r.post.id, r.tipo
            """)
    List<Object[]> contarReaccionesPorPosts(@Param("postIds") List<Long> postIds);

    /**
     * NUEVO: Cuenta reacciones de varios comentarios en 1 query.
     * Devuelve [comentarioId, tipo, count].
     */
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

    /**
     * NUEVO: Reacciones de un usuario a posts (paginado).
     * Útil para "mis reacciones".
     */
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

    /**
     * NUEVO: Reacciones de un usuario a comentarios (paginado).
     */
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

    /**
     * NUEVO: Devuelve los IDs de posts donde el usuario reaccionó.
     * Útil para marcar los posts del feed con el estado "ya reaccioné".
     */
    @Query("""
            SELECT r.post.id FROM Reaccion r
            WHERE r.usuario.id = :usuarioId
              AND r.post.id IN :postIds
            """)
    List<Long> findPostsDondeReaccione(
            @Param("usuarioId") Long usuarioId,
            @Param("postIds") List<Long> postIds
    );

    /**
     * NUEVO: Devuelve los IDs de comentarios donde el usuario reaccionó.
     */
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

    /**
     * Elimina la reacción de un usuario a un post.
     * MEJORA: usa @Modifying para evitar el SELECT + DELETE.
     */
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

    /**
     * Elimina la reacción de un usuario a un comentario.
     */
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

    /**
     * NUEVO: Elimina TODAS las reacciones de un post (bulk).
     * Útil cuando se elimina un post (aunque normalmente es CASCADE).
     */
    @Modifying
    @Query("DELETE FROM Reaccion r WHERE r.post.id = :postId")
    int deleteByPostId(@Param("postId") Long postId);

    /**
     *NUEVO: Elimina TODAS las reacciones de un comentario.
     */
    @Modifying
    @Query("DELETE FROM Reaccion r WHERE r.comentario.id = :comentarioId")
    int deleteByComentarioId(@Param("comentarioId") Long comentarioId);

    // ============================================================
    // TOP POSTS REACCIONADOS
    // ============================================================

    /**
     * NUEVO: Top N posts con más reacciones.
     * Devuelve [postId, totalReacciones].
     */
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

    /**
     * NUEVO: Tipos de reacción distintos que existen en el sistema.
     * Útil para que el frontend sepa qué emojis mostrar.
     */
    @Query("""
            SELECT DISTINCT r.tipo FROM Reaccion r
            ORDER BY r.tipo ASC
            """)
    List<String> findDistinctTipos();

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * NUEVO: Total de reacciones en el sistema.
     */
    @Query("SELECT COUNT(r) FROM Reaccion r")
    long countTotal();

    /**
     * NUEVO: Cuenta reacciones agrupadas por tipo (global).
     * Devuelve [tipo, count].
     */
    @Query("""
            SELECT r.tipo, COUNT(r)
            FROM Reaccion r
            GROUP BY r.tipo
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> contarGlobalPorTipo();

    /**
     * NUEVO: Reacciones creadas en un rango de fechas.
     */
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
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     *  NUEVO: Elimina reacciones huérfanas
     * (sin post ni comentario, no debería pasar por el CHECK constraint).
     */
    @Modifying
    @Query("""
            DELETE FROM Reaccion r
            WHERE r.post IS NULL
              AND r.comentario IS NULL
            """)
    int eliminarHuerfanas();
}