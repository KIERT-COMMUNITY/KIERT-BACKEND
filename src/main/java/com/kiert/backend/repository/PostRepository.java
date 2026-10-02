// src/main/java/com/kiert/backend/repository/PostRepository.java
package com.kiert.backend.repository;

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

    // ============================================================
    // FEED — LISTADO GENERAL
    // ============================================================

    /**
     * ⚠️ LEGACY: Carga TODOS los posts activos (con autor, personalización y adjuntos).
     * Usar solo en casos puntuales. Para el feed usar `findAllActivePaginado`.
     */
    @Deprecated
    @Query("""
            SELECT DISTINCT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            LEFT JOIN FETCH p.adjuntos
            WHERE p.eliminado = false
            ORDER BY p.fechaCreacion DESC
            """)
    List<Post> findAllActiveOrderByFechaCreacionDesc();

    /**
     * ✅ NUEVO: Feed paginado SIN adjuntos (evita el bug de HHH000104).
     * Los adjuntos se cargan en una query separada (`findAdjuntosByPostIds`).
     */
    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            WHERE p.eliminado = false
            ORDER BY p.fechaCreacion DESC
            """)
    Page<Post> findAllActivePaginado(Pageable pageable);

    // ============================================================
    // OBTENER POR ID
    // ============================================================

    /**
     * Carga un post por ID con autor, personalización y adjuntos.
     */
    @Query("""
            SELECT DISTINCT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            LEFT JOIN FETCH p.adjuntos
            WHERE p.id = :id
              AND p.eliminado = false
            """)
    Optional<Post> findActiveById(@Param("id") Long id);

    /**
     * Verifica si existe un post activo.
     */
    @Query("""
            SELECT COUNT(p) > 0 FROM Post p
            WHERE p.id = :id
              AND p.eliminado = false
            """)
    boolean existsActiveById(@Param("id") Long id);

    // ============================================================
    // POSTS POR CATEGORÍA
    // ============================================================

    /**
     * ✅ NUEVO: Posts por categoría paginados (sin adjuntos).
     */
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

    /**
     * ✅ NUEVO: Categorías distintas con al menos 1 post activo.
     */
    @Query("""
            SELECT DISTINCT p.categoria FROM Post p
            WHERE p.eliminado = false
            ORDER BY p.categoria ASC
            """)
    List<String> findDistinctCategorias();

    // ============================================================
    // POSTS POR USUARIO
    // ============================================================

    /**
     * ✅ NUEVO: Posts de un usuario paginados (para el perfil).
     */
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

    /**
     * ✅ NUEVO: Cuenta posts activos de un usuario.
     */
    @Query("""
            SELECT COUNT(p) FROM Post p
            WHERE p.autor.id = :usuarioId
              AND p.eliminado = false
            """)
    long countByAutorId(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // BÚSQUEDA POR TEXTO
    // ============================================================

    /**
     * ✅ NUEVO: Buscar posts por título o descripción.
     */
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

    // ============================================================
    // POSTS CON ADJUNTOS (para cargar en 1 query tras paginar)
    // ============================================================

    /**
     * ✅ CRÍTICO: Carga los adjuntos de varios posts en 1 query.
     * Se usa DESPUÉS de `findAllActivePaginado` para evitar el bug HHH000104.
     */
    @Query("""
            SELECT DISTINCT p FROM Post p
            LEFT JOIN FETCH p.adjuntos
            WHERE p.id IN :postIds
            """)
    List<Post> findAdjuntosByPostIds(@Param("postIds") List<Long> postIds);

    // ============================================================
    // TOP POSTS (home, recomendaciones)
    // ============================================================

    /**
     * ✅ NUEVO: Top N posts con más compartidos.
     */
    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            WHERE p.eliminado = false
            ORDER BY p.totalCompartidos DESC, p.fechaCreacion DESC
            """)
    List<Post> findTopMasCompartidos(Pageable pageable);

    /**
     * ✅ NUEVO: Posts más recientes (para home).
     */
    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor a
            LEFT JOIN FETCH a.personalizacion
            WHERE p.eliminado = false
            ORDER BY p.fechaCreacion DESC
            """)
    List<Post> findRecientes(Pageable pageable);

    // ============================================================
    // POSTS POR ADJUNTO (galería)
    // ============================================================

    /**
     * ✅ NUEVO: Posts que tienen adjuntos de un tipo específico.
     * Útil para "galería de imágenes" o "videos".
     */
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

    // ============================================================
    // CONTADORES AGRUPADOS
    // ============================================================

    /**
     * ✅ NUEVO: Cuenta posts agrupados por categoría.
     * Devuelve [categoria, count].
     */
    @Query("""
            SELECT p.categoria, COUNT(p)
            FROM Post p
            WHERE p.eliminado = false
            GROUP BY p.categoria
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> contarPorCategoria();

    /**
     * ✅ NUEVO: Cuenta posts por usuario (bulk, para feed).
     * Devuelve [usuarioId, count].
     */
    @Query("""
            SELECT p.autor.id, COUNT(p)
            FROM Post p
            WHERE p.autor.id IN :usuarioIds
              AND p.eliminado = false
            GROUP BY p.autor.id
            """)
    List<Object[]> contarPorUsuarios(@Param("usuarioIds") List<Long> usuarioIds);

    // ============================================================
    // ADMIN / MODERACIÓN
    // ============================================================

    /**
     * ✅ NUEVO: Todos los posts (incluyendo eliminados) para admin.
     */
    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor
            ORDER BY p.fechaCreacion DESC
            """)
    Page<Post> findAllParaAdmin(Pageable pageable);

    /**
     * ✅ NUEVO: Posts eliminados (soft-deleted) para restaurar.
     */
    @Query("""
            SELECT p FROM Post p
            LEFT JOIN FETCH p.autor
            WHERE p.eliminado = true
            ORDER BY p.fechaEliminacion DESC
            """)
    Page<Post> findEliminados(Pageable pageable);

    /**
     * ✅ NUEVO: Restaurar un post eliminado.
     */
    @Modifying
    @Query("""
            UPDATE Post p
            SET p.eliminado = false,
                p.fechaEliminacion = null
            WHERE p.id = :id
            """)
    int restaurarPost(@Param("id") Long id);

    // ============================================================
    // BULK UPDATES
    // ============================================================

    /**
     * ✅ NUEVO: Soft-delete en lote (útil para moderación).
     */
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

    /**
     * ✅ NUEVO: Elimina posts borrados hace más de N días (hard delete).
     */
    @Modifying
    @Query(value = """
            DELETE FROM posts
            WHERE eliminado = true
              AND fecha_eliminacion < :limite
            """, nativeQuery = true)
    int eliminarPostsBorradosAntiguos(@Param("limite") Instant limite);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * ✅ NUEVO: Total de posts activos.
     */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.eliminado = false")
    long countActivos();

    /**
     * ✅ NUEVO: Total de posts creados en un rango de fechas (para reportes).
     */
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
}