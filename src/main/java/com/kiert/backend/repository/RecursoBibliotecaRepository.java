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
     * Lista todos los recursos activos (SIN usuario para recursos globales).
     * ⚠️ Usa la versión paginada en el feed.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
            ORDER BY r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findAllActiveOrderByFechaAgregadoDesc();

    /**
     *  NUEVO: Versión paginada (recomendada).
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
            ORDER BY r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> findAllActivePaginado(Pageable pageable);

    /**
     * Recursos por categoría (SIN usuario).
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.categoria = :categoria
            ORDER BY r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findByCategoriaOrderByFechaAgregadoDesc(
            @Param("categoria") String categoria
    );

    /**
     *  NUEVO: Recursos por categoría paginados.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.categoria = :categoria
            ORDER BY r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> findByCategoriaPaginado(
            @Param("categoria") String categoria,
            Pageable pageable
    );

    /**
     * Recursos destacados (SIN usuario).
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.destacado = true
            ORDER BY r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findDestacados();

    /**
     *  NUEVO: Recursos destacados paginados.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.destacado = true
            ORDER BY r.fechaAgregado DESC
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

    /**
     *  NUEVO: Categorías + nivel (para filtros combinados).
     */
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
    // LISTADOS QUE INCLUYEN LOS RECURSOS DEL USUARIO
    // ============================================================

    /**
     * Recursos GLOBALES + los del usuario indicado.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
              AND (r.usuario IS NULL OR r.usuario.id = :usuarioId)
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findVisiblesParaUsuario(@Param("usuarioId") Long usuarioId);

    /**
     *  NUEVO: Versión paginada.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
              AND (r.usuario IS NULL OR r.usuario.id = :usuarioId)
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> findVisiblesParaUsuarioPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    /**
     * Recursos visibles por categoría.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
              AND r.categoria = :categoria
              AND (r.usuario IS NULL OR r.usuario.id = :usuarioId)
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findVisiblesPorCategoria(
            @Param("usuarioId") Long usuarioId,
            @Param("categoria") String categoria
    );

    /**
     *  NUEVO: Visibles por categoría paginados.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
              AND r.categoria = :categoria
              AND (r.usuario IS NULL OR r.usuario.id = :usuarioId)
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> findVisiblesPorCategoriaPaginado(
            @Param("usuarioId") Long usuarioId,
            @Param("categoria") String categoria,
            Pageable pageable
    );

    // ============================================================
    // RECURSOS POR USUARIO
    // ============================================================

    /**
     * Recursos creados por un usuario específico.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.usuario.id = :usuarioId
            ORDER BY r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     *  NUEVO: Versión paginada.
     */
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

    /**
     * NUEVO: Cuenta recursos de un usuario.
     */
    @Query("""
            SELECT COUNT(r) FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.usuario.id = :usuarioId
            """)
    long countByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Comprueba si un recurso pertenece a un usuario y está activo.
     */
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

    /**
     * NUEVO: Obtiene un recurso activo por ID con su creador cargado.
     */
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

    /**
     * Busca recursos por texto en múltiples campos.
     * ⚠️ LOWER + LIKE '%...%' es lento en tablas grandes.
     * Si es frecuente, considera FULLTEXT.
     */
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

    /**
     * NUEVO: Búsqueda paginada.
     */
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

    /**
     * Búsqueda por categoría.
     */
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
     * Búsqueda que incluye los recursos del usuario.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
              AND (r.usuario IS NULL OR r.usuario.id = :usuarioId)
              AND (
                LOWER(r.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.autor) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.plataforma) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.subcategoria) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.tags) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> buscarVisiblesParaUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("query") String query
    );

    /**
     * NUEVO: Búsqueda visible para usuario, paginada.
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario u
            WHERE r.activo = true
              AND (r.usuario IS NULL OR r.usuario.id = :usuarioId)
              AND (
                LOWER(r.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.autor) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.plataforma) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.subcategoria) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(r.tags) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
            """)
    Page<RecursoBiblioteca> buscarVisiblesParaUsuarioPaginado(
            @Param("usuarioId") Long usuarioId,
            @Param("query") String query,
            Pageable pageable
    );

    // ============================================================
    // TOP / HOME
    // ============================================================

    /**
     * NUEVO: Recursos recientes (para home).
     */
    @Query("""
            SELECT r FROM RecursoBiblioteca r
            LEFT JOIN FETCH r.usuario
            WHERE r.activo = true
              AND r.esUsuario = false
            ORDER BY r.fechaAgregado DESC
            """)
    List<RecursoBiblioteca> findRecientes(Pageable pageable);

    /**
     * NUEVO: Recursos destacados recientes (para home).
     */
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

    /**
     * NUEVO: Cuenta recursos agrupados por categoría.
     * Devuelve [categoria, count].
     */
    @Query("""
            SELECT r.categoria, COUNT(r)
            FROM RecursoBiblioteca r
            WHERE r.activo = true
            GROUP BY r.categoria
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> contarPorCategoria();

    /**
     * NUEVO: Cuenta recursos agrupados por nivel.
     * Devuelve [nivel, count].
     */
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

    /**
     * NUEVO: Verifica si existe un recurso activo con ese título
     * para el mismo usuario (anti-duplicados).
     */
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

    /**
     * NUEVO: Desactiva varios recursos en 1 query.
     */
    @Modifying
    @Query("""
            UPDATE RecursoBiblioteca r
            SET r.activo = false
            WHERE r.id IN :ids
            """)
    int desactivarEnLote(@Param("ids") List<Long> ids);

    /**
     * NUEVO: Marca/desmarca varios recursos como destacados.
     */
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
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * NUEVO: Elimina (hard delete) recursos desactivados hace más de N días.
     */
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

    /**
     *  NUEVO: Total de recursos activos.
     */
    @Query("SELECT COUNT(r) FROM RecursoBiblioteca r WHERE r.activo = true")
    long countActivos();

    /**
     *  NUEVO: Total de recursos globales activos.
     */
    @Query("""
            SELECT COUNT(r) FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.esUsuario = false
            """)
    long countGlobalesActivos();

    /**
     *NUEVO: Total de recursos subidos por usuarios activos.
     */
    @Query("""
            SELECT COUNT(r) FROM RecursoBiblioteca r
            WHERE r.activo = true
              AND r.esUsuario = true
            """)
    long countDeUsuariosActivos();
}