// src/main/java/com/kiert/backend/repository/RecursoBibliotecaRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.RecursoBiblioteca;
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
public interface RecursoBibliotecaRepository extends JpaRepository<RecursoBiblioteca, Long> {

    // ============================================================
    // LISTADOS GLOBALES
    // ============================================================

    /**
     * Lista TODOS los recursos activos (globales + de todos los usuarios).
     * ✅ FIX: incluye recursos de todos los usuarios, no solo los propios.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findAllActiveOrderByFechaAgregadoDesc();

    /**
     * Versión paginada.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> findAllActivePaginado(Pageable pageable);

    /**
     * Recursos por categoría (TODOS los usuarios).
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.categoria = :categoria
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findByCategoriaOrderByFechaAgregadoDesc(
            @Param("categoria") String categoria
    );

    /**
     * Recursos por categoría paginados.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.categoria = :categoria
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> findByCategoriaPaginado(
            @Param("categoria") String categoria,
            Pageable pageable
    );

    /**
     * Recursos destacados.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.destacado = true
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findDestacados();

    /**
     * Recursos destacados paginados.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.destacado = true
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> findDestacadosPaginado(Pageable pageable);

    // ============================================================
    // CATEGORÍAS Y NIVELES
    // ============================================================

    @Query("""
            SELECT DISTINCT r.categoria FROM RecursoBiblioteca r
            WHERE r.activo = true
            ORDER BY r.categoria ASC
            """)
    List<String> findDistinctCategorias();

    @Query("""
            SELECT DISTINCT r.nivel FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.nivel IS NOT NULL
              AND r.nivel <> ''
            ORDER BY r.nivel ASC
            """)
    List<String> findDistinctNiveles();

    @Query("""
            SELECT DISTINCT r.subcategoria FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.categoria = :categoria
              AND r.subcategoria IS NOT NULL
              AND r.subcategoria <> ''
            ORDER BY r.subcategoria ASC
            """)
    List<String> findDistinctSubcategoriasByCategoria(@Param("categoria") String categoria);

    // ============================================================
    // LISTADOS VISIBLES PARA EL USUARIO (TODOS los activos)
    // ✅ FIX: ahora muestra recursos de TODOS los usuarios
    // ============================================================

    /**
     * ✅ Muestra TODOS los recursos activos.
     * Los del usuario indicado aparecen primero (para UX).
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
            ORDER BY 
              CASE WHEN r.usuario.id = :usuarioId THEN 0 ELSE 1 END ASC,
              r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findVisiblesParaUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Versión paginada.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
            ORDER BY 
              CASE WHEN r.usuario.id = :usuarioId THEN 0 ELSE 1 END ASC,
              r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> findVisiblesParaUsuarioPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    /**
     * ✅ Recursos visibles por categoría (TODOS los usuarios).
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
              AND r.categoria = :categoria
            ORDER BY 
              CASE WHEN r.usuario.id = :usuarioId THEN 0 ELSE 1 END ASC,
              r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findVisiblesPorCategoria(
            @Param("usuarioId") Long usuarioId,
            @Param("categoria") String categoria
    );

    /**
     * Visibles por categoría paginados.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
              AND r.categoria = :categoria
            ORDER BY 
              CASE WHEN r.usuario.id = :usuarioId THEN 0 ELSE 1 END ASC,
              r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> findVisiblesPorCategoriaPaginado(
            @Param("usuarioId") Long usuarioId,
            @Param("categoria") String categoria,
            Pageable pageable
    );

    // ============================================================
    // RECURSOS POR USUARIO
    // ============================================================

    @Query("""
            SELECT r FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.usuario.id = :usuarioId
            ORDER BY r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT r FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.usuario.id = :usuarioId
            ORDER BY r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> findByUsuarioIdPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(r) FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.usuario.id = :usuarioId
            """)
    long countByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT COUNT(r) > 0 FROM RecursoBiblioteca r
            WHERE r.id = :id
              AND r.usuario.id = :usuarioId
              AND r.activo = true
            """)
    boolean existsByIdAndUsuarioId(
            @Param("id") Long id,
            @Param("usuarioId") Long usuarioId
    );

    // ============================================================
    // OBTENER POR ID
    // ============================================================

    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.id = :id
              AND r.activo = true
            """)
    Optional<RecursoBiblioteca> findActivoById(@Param("id") Long id);

    // ============================================================
    // BÚSQUEDA
    // ============================================================

    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND (
                LOWER(r.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.autor) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.subcategoria) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.tags) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> buscar(@Param("query") String query);

    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND (
                LOWER(r.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.autor) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.subcategoria) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.tags) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> buscarPaginado(
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.categoria = :categoria
              AND (
                LOWER(r.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> buscarPorCategoria(
            @Param("categoria") String categoria,
            @Param("query") String query
    );

    /**
     * ✅ Búsqueda que incluye recursos de TODOS los usuarios.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
              AND (
                LOWER(r.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.autor) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.plataforma) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.subcategoria) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.tags) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY 
              CASE WHEN r.usuario.id = :usuarioId THEN 0 ELSE 1 END ASC,
              r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> buscarVisiblesParaUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("query") String query
    );

    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
              AND (
                LOWER(r.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.autor) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.plataforma) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.subcategoria) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.tags) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY 
              CASE WHEN r.usuario.id = :usuarioId THEN 0 ELSE 1 END ASC,
              r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> buscarVisiblesParaUsuarioPaginado(
            @Param("usuarioId") Long usuarioId,
            @Param("query") String query,
            Pageable pageable
    );

    // ============================================================
    // TOP / HOME
    // ============================================================

    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.esUsuario = false
            ORDER BY r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findRecientes(Pageable pageable);

    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.esUsuario = false
              AND r.destacado = true
            ORDER BY r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findDestacadosRecientes(Pageable pageable);

    // ============================================================
    // CONTADORES AGRUPADOS
    // ============================================================

    @Query("""
            SELECT r.categoria, COUNT(r)
            FROM RecursoBiblioteca r
            WHERE r.activo = true
            GROUP BY r.categoria
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> contarPorCategoria();

    @Query("""
            SELECT r.nivel, COUNT(r)
            FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.nivel IS NOT NULL
            GROUP BY r.nivel
            """)
    List<Object[]> contarPorNivel();

    // ============================================================
    // VALIDACIONES
    // ============================================================

    @Query("""
            SELECT COUNT(r) > 0 FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.usuario.id = :usuarioId
              AND LOWER(r.titulo) = LOWER(:titulo)
            """)
    boolean existePorUsuarioYTitulo(
            @Param("usuarioId") Long usuarioId,
            @Param("titulo") String titulo
    );

    // ============================================================
    // BULK UPDATES
    // ============================================================

    @Modifying
    @Query("""
            UPDATE RecursoBiblioteca r
            SET r.activo = false
            WHERE r.id IN :ids
            """)
    int desactivarEnLote(@Param("ids") List<Long> ids);

    @Modifying
    @Query("""
            UPDATE RecursoBiblioteca r
            SET r.destacado = :destacado
            WHERE r.id IN :ids
            """)
    int marcarDestacados(
            @Param("ids") List<Long> ids,
            @Param("destacado") Boolean destacado
    );

    // ============================================================
    // LIMPIEZA
    // ============================================================

    @Modifying
    @Query(value = """
            DELETE FROM recursos_biblioteca
            WHERE activo = false
              AND fecha_actualizacion < :limite
            """, nativeQuery = true)
    int eliminarDesactivadosAntiguos(@Param("limite") Instant limite);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    @Query("SELECT COUNT(r) FROM RecursoBiblioteca r WHERE r.activo = true")
    long countActivos();

    @Query("""
            SELECT COUNT(r) FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.esUsuario = false
            """)
    long countGlobalesActivos();

    @Query("""
            SELECT COUNT(r) FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.esUsuario = true
            """)
    long countDeUsuariosActivos();
}