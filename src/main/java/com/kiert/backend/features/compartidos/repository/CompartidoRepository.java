// src/main/java/com/kiert/backend/repository/CompartidoRepository.java
package com.kiert.backend.features.compartidos.repository;

import com.kiert.backend.entity.Compartido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompartidoRepository extends JpaRepository<Compartido, Long> {

    @Query("""
            SELECT c FROM Compartido c
            LEFT JOIN FETCH c.usuario
            LEFT JOIN FETCH c.post
            WHERE c.post.id = :postId
            ORDER BY c.fechaCreacion DESC
            """)
    List<Compartido> findByPostId(@Param("postId") Long postId);

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

    @Query("""
            SELECT c FROM Compartido c
            LEFT JOIN FETCH c.usuario
            WHERE c.post.id = :postId
              AND c.tipoCompartido = 'INTERNO'
            ORDER BY c.fechaCreacion DESC
            """)
    List<Compartido> findInternosByPostId(@Param("postId") Long postId);

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

    long countByPostId(Long postId);

    @Query("""
            SELECT COUNT(c) FROM Compartido c
            WHERE c.post.id = :postId
              AND c.tipoCompartido = 'INTERNO'
            """)
    long countInternosByPostId(@Param("postId") Long postId);

    @Query("""
            SELECT COUNT(c) FROM Compartido c
            WHERE c.usuario.id = :usuarioId
            """)
    long countByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT c.post.id, COUNT(c)
            FROM Compartido c
            WHERE c.post.id IN :postIds
            GROUP BY c.post.id
            """)
    List<Object[]> contarCompartidosPorPosts(@Param("postIds") List<Long> postIds);

    boolean existsByUsuarioIdAndPostId(Long usuarioId, Long postId);

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

    @Query("""
            SELECT c.tipoCompartido, COUNT(c)
            FROM Compartido c
            WHERE c.fechaCreacion >= :desde
            GROUP BY c.tipoCompartido
            """)
    List<Object[]> contarPorTipoDesde(@Param("desde") Instant desde);

    @Query("""
            SELECT c.post.id, COUNT(c) as total
            FROM Compartido c
            WHERE c.fechaCreacion >= :desde
            GROUP BY c.post.id
            ORDER BY total DESC
            """)
    List<Object[]> topPostsMasCompartidos(
            @Param("desde") Instant desde,
            Pageable pageable
    );

    // ============================================================
    // ANTI-SPAM: Verificar si el usuario ya compartió recientemente
    // ============================================================

    /**
     * NUEVO: Verifica si el usuario ya compartió el post en las últimas N horas.
     * Se usa para prevenir spam de compartidos.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM Compartido c
            WHERE c.usuario.id = :usuarioId
              AND c.post.id = :postId
              AND c.fechaCreacion >= :desde
            """)
    boolean compartioRecientemente(
            @Param("usuarioId") Long usuarioId,
            @Param("postId") Long postId,
            @Param("desde") Instant desde
    );
}