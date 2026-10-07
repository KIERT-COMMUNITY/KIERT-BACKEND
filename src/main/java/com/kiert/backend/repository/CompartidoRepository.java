// src/main/java/com/kiert/backend/repository/CompartidoRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Compartido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompartidoRepository extends JpaRepository<Compartido, Long> {

    // ============================================================
    // COMPARTIDOS POR POST
    // ============================================================

    /**
     * Lista todos los compartidos de un post (con usuario y post cargados).
     * ⚠️ Para posts virales, usa `findByPostIdPaginado`.
     */
    @Query("""
            SELECT c FROM Compartido c
            LEFT JOIN FETCH c.usuario
            LEFT JOIN FETCH c.post
            WHERE c.post.id = :postId
            ORDER BY c.fechaCreacion DESC
            """)
    List<Compartido> findByPostId(@Param("postId") Long postId);

    /**
     *  NUEVO: Versión paginada (recomendada para posts virales).
     */
    @Query("""
            SELECT c FROM Compartido c
            LEFT JOIN FETCH c.usuario
            LEFT JOIN FETCH c.post
            WHERE c.post.id = :postId
            ORDER BY c.fechaCreacion DESC
            """)
    Page<Compartido> findByPostIdPaginado(
            @Param("postId") Long postId,
            Pageable pageable
    );

    /**
     *  NUEVO: Compartidos INTERNOS (dentro de la app) de un post.
     * Los EXTERNOS no cuentan para la lista de "quién compartió".
     */
    @Query("""
            SELECT c FROM Compartido c
            LEFT JOIN FETCH c.usuario
            WHERE c.post.id = :postId
              AND c.tipoCompartido = 'INTERNO'
            ORDER BY c.fechaCreacion DESC
            """)
    List<Compartido> findInternosByPostId(@Param("postId") Long postId);

    // ============================================================
    // COMPARTIDOS POR USUARIO
    // ============================================================

    /**
     *  NUEVO: Últimos compartidos de un usuario (para el perfil).
     */
    @Query("""
            SELECT c FROM Compartido c
            LEFT JOIN FETCH c.post p
            WHERE c.usuario.id = :usuarioId
              AND p.eliminado = false
            ORDER BY c.fechaCreacion DESC
            """)
    Page<Compartido> findUltimosCompartidosDeUsuario(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    /**
     *  NUEVO: Compartidos de un usuario en un post específico.
     * Útil para "mis compartidos de este post".
     */
    @Query("""
            SELECT c FROM Compartido c
            WHERE c.usuario.id = :usuarioId
              AND c.post.id = :postId
            ORDER BY c.fechaCreacion DESC
            """)
    List<Compartido> findByUsuarioAndPost(
            @Param("usuarioId") Long usuarioId,
            @Param("postId") Long postId
    );

    // ============================================================
    // CONTADORES
    // ============================================================

    long countByPostId(Long postId);

    /**
     *  NUEVO: Contar compartidos INTERNOS de un post (excluye externos).
     */
    @Query("""
            SELECT COUNT(c) FROM Compartido c
            WHERE c.post.id = :postId
              AND c.tipoCompartido = 'INTERNO'
            """)
    long countInternosByPostId(@Param("postId") Long postId);

    /**
     *  NUEVO: Contar TODOS los compartidos de un usuario.
     * Útil para estadísticas del perfil.
     */
    @Query("""
            SELECT COUNT(c) FROM Compartido c
            WHERE c.usuario.id = :usuarioId
            """)
    long countByUsuarioId(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // CONTADORES POR POSTS (para el feed — elimina N+1)
    // ============================================================

    /**
     *  NUEVO: Cuenta compartidos agrupados por post.
     * Devuelve [postId, count].
     *
     * Uso en el feed: 1 sola query para N posts.
     */
    @Query("""
            SELECT c.post.id, COUNT(c)
            FROM Compartido c
            WHERE c.post.id IN :postIds
            GROUP BY c.post.id
            """)
    List<Object[]> contarCompartidosPorPosts(@Param("postIds") List<Long> postIds);

    // ============================================================
    // VERIFICAR SI USUARIO COMPARTIÓ
    // ============================================================

    /**
     * Verifica si un usuario ya compartió un post.
     * Útil para el botón "Compartir" en el frontend.
     */
    boolean existsByUsuarioIdAndPostId(Long usuarioId, Long postId);

    /**
     *  NUEVO: Devuelve el compartido específico si existe.
     */
    @Query("""
            SELECT c FROM Compartido c
            WHERE c.usuario.id = :usuarioId
              AND c.post.id = :postId
            ORDER BY c.fechaCreacion DESC
            """)
    Optional<Compartido> findUltimoByUsuarioAndPost(
            @Param("usuarioId") Long usuarioId,
            @Param("postId") Long postId
    );

    // ============================================================
    // AUDITORÍA / REPORTES
    // ============================================================

    /**
     *  NUEVO: Contar compartidos por tipo (INTERNO vs EXTERNO) en un rango de fechas.
     * Útil para reportes.
     */
    @Query("""
            SELECT c.tipoCompartido, COUNT(c)
            FROM Compartido c
            WHERE c.fechaCreacion >= :desde
            GROUP BY c.tipoCompartido
            """)
    List<Object[]> contarPorTipoDesde(@Param("desde") java.time.Instant desde);

    /**
     *  NUEVO: Top N posts más compartidos en un rango de fechas.
     */
    @Query("""
            SELECT c.post.id, COUNT(c) as total
            FROM Compartido c
            WHERE c.fechaCreacion >= :desde
            GROUP BY c.post.id
            ORDER BY total DESC
            """)
    List<Object[]> topPostsMasCompartidos(
            @Param("desde") java.time.Instant desde,
            Pageable pageable
    );
}