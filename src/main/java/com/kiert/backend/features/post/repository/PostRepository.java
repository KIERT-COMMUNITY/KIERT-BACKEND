// src/main/java/com/kiert/backend/features/post/repository/PostRepository.java
package com.kiert.backend.features.post.repository;

import com.kiert.backend.entity.Post;
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
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Lista todos los posts activos ordenados por fecha de creacion descendente.
     *
     * <p>NOTA: antes estaba marcado con {@code @Deprecated} por error. La consulta
     * es valida y se usa en PostService.listar(). Si en el futuro quieres
     * paginar, usa {@link #findAllActivePaginado(Pageable)} en su lugar.</p>
     */
    @Query("""
            SELECT DISTINCT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            LEFT JOIN FETCH p.adjuntos
            WHERE p.eliminado = false
            ORDER BY p.fechaCreacion DESC
            """)
    List<Post> findAllActiveOrderByFechaCreacionDesc();

    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            WHERE p.eliminado = false
            ORDER BY p.fechaCreacion DESC
            """)
    Page<Post> findAllActivePaginado(Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            LEFT JOIN FETCH p.adjuntos
            WHERE p.id = :id
              AND p.eliminado = false
            """)
    Optional<Post> findActiveById(@Param("id") Long id);

    @Query("""
            SELECT COUNT(p) > 0 FROM Post p
            WHERE p.id = :id
              AND p.eliminado = false
            """)
    boolean existsActiveById(@Param("id") Long id);

    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            WHERE p.categoria = :categoria
              AND p.eliminado = false
            ORDER BY p.fechaCreacion DESC
            """)
    Page<Post> findByCategoriaPaginado(
            @Param("categoria") String categoria,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT p.categoria FROM Post p
            WHERE p.eliminado = false
            ORDER BY p.categoria ASC
            """)
    List<String> findDistinctCategorias();

    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            WHERE p.autor.id = :usuarioId
              AND p.eliminado = false
            ORDER BY p.fechaCreacion DESC
            """)
    Page<Post> findByAutorPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(p) FROM Post p
            WHERE p.autor.id = :usuarioId
              AND p.eliminado = false
            """)
    long countByAutorId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            WHERE p.eliminado = false
              AND (
                LOWER(p.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY p.fechaCreacion DESC
            """)
    Page<Post> buscar(
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT p FROM Post p
            LEFT JOIN FETCH p.adjuntos
            WHERE p.id IN :postIds
            """)
    List<Post> findAdjuntosByPostIds(@Param("postIds") List<Long> postIds);

    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            WHERE p.eliminado = false
            ORDER BY p.totalCompartidos DESC, p.fechaCreacion DESC
            """)
    List<Post> findTopMasCompartidos(Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            WHERE p.eliminado = false
            ORDER BY p.fechaCreacion DESC
            """)
    List<Post> findRecientes(Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM Post p
            LEFT JOIN FETCH p.autor a
            WHERE p.eliminado = false
              AND EXISTS (
                SELECT 1 FROM Adjunto adj
                WHERE adj.post = p AND adj.tipo = :tipo
              )
            ORDER BY p.fechaCreacion DESC
            """)
    Page<Post> findConAdjuntosDeTipo(
            @Param("tipo") String tipo,
            Pageable pageable
    );

    @Query("""
            SELECT p.categoria, COUNT(p)
            FROM Post p
            WHERE p.eliminado = false
            GROUP BY p.categoria
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> contarPorCategoria();

    @Query("""
            SELECT p.autor.id, COUNT(p)
            FROM Post p
            WHERE p.autor.id IN :usuarioIds
              AND p.eliminado = false
            GROUP BY p.autor.id
            """)
    List<Object[]> contarPorUsuarios(@Param("usuarioIds") List<Long> usuarioIds);

    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor
            ORDER BY p.fechaCreacion DESC
            """)
    Page<Post> findAllParaAdmin(Pageable pageable);

    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor
            WHERE p.eliminado = true
            ORDER BY p.fechaEliminacion DESC
            """)
    Page<Post> findEliminados(Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Post p
            SET p.eliminado = false,
                p.fechaEliminacion = null
            WHERE p.id = :id
            """)
    int restaurarPost(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE Post p
            SET p.eliminado = true,
                p.fechaEliminacion = :fecha
            WHERE p.id IN :ids
              AND p.eliminado = false
            """)
    int eliminarEnLote(
            @Param("ids") List<Long> ids,
            @Param("fecha") Instant fecha
    );

    @Modifying
    @Query(value = """
            DELETE FROM posts
            WHERE eliminado = true
              AND fecha_eliminacion < :limite
            """, nativeQuery = true)
    int eliminarPostsBorradosAntiguos(@Param("limite") Instant limite);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.eliminado = false")
    long countActivos();

    @Query("""
            SELECT COUNT(p) FROM Post p
            WHERE p.fechaCreacion >= :desde
              AND p.fechaCreacion < :hasta
              AND p.eliminado = false
            """)
    long countEnRango(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );

    // ============================================================
    // SEGURIDAD: Validar autoria
    // ============================================================

    /**
     * Verifica si un usuario es el autor del post.
     * Se usa para prevenir que un usuario edite/elimine posts ajenos.
     */
    @Query("""
            SELECT COUNT(p) > 0 FROM Post p
            WHERE p.id = :postId
              AND p.autor.id = :usuarioId
              AND p.eliminado = false
            """)
    boolean esAutorDelPost(
            @Param("postId") Long postId,
            @Param("usuarioId") Long usuarioId
    );
}