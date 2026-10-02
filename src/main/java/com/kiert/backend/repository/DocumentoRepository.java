// src/main/java/com/kiert/backend/repository/DocumentoRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Documento;
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
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    // ============================================================
    // LISTAR DOCUMENTOS
    // ============================================================

    /**
     * Lista todos los documentos activos ordenados por fecha DESC.
     * ⚠️ Para muchos documentos, usa `findAllActivePaginado`.
     */
    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
            ORDER BY d.fechaCreacion DESC
            """)
    List<Documento> findAllActiveOrderByFechaCreacionDesc();

    /**
     * ✅ NUEVO: Versión paginada (recomendada).
     */
    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
            ORDER BY d.fechaCreacion DESC
            """)
    Page<Documento> findAllActivePaginado(Pageable pageable);

    // ============================================================
    // LISTAR POR CATEGORÍA
    // ============================================================

    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
              AND d.categoria = :categoria
            ORDER BY d.fechaCreacion DESC
            """)
    List<Documento> findByCategoriaOrderByFechaCreacionDesc(@Param("categoria") String categoria);

    /**
     * ✅ NUEVO: Versión paginada por categoría.
     */
    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
              AND d.categoria = :categoria
            ORDER BY d.fechaCreacion DESC
            """)
    Page<Documento> findByCategoriaPaginado(
            @Param("categoria") String categoria,
            Pageable pageable
    );

    // ============================================================
    // CATEGORÍAS
    // ============================================================

    /**
     * Obtiene las categorías distintas (estándar + personalizadas).
     * ✅ MEJORA: incluye `categoriaPersonalizada` cuando no es null.
     */
    @Query("""
            SELECT DISTINCT d.categoria FROM Documento d
            WHERE d.activo = true
            UNION
            SELECT DISTINCT d.categoriaPersonalizada FROM Documento d
            WHERE d.activo = true
              AND d.categoriaPersonalizada IS NOT NULL
              AND d.categoriaPersonalizada <> ''
            ORDER BY 1 ASC
            """)
    List<String> findDistinctCategorias();

    // ============================================================
    // DOCUMENTOS POR USUARIO
    // ============================================================

    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
              AND d.usuario.id = :usuarioId
            ORDER BY d.fechaCreacion DESC
            """)
    List<Documento> findByUsuarioIdOrderByFechaCreacionDesc(@Param("usuarioId") Long usuarioId);

    /**
     * ✅ NUEVO: Versión paginada de documentos por usuario.
     */
    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
              AND d.usuario.id = :usuarioId
            ORDER BY d.fechaCreacion DESC
            """)
    Page<Documento> findByUsuarioIdPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    /**
     * ✅ NUEVO: Contar documentos de un usuario.
     */
    @Query("""
            SELECT COUNT(d) FROM Documento d
            WHERE d.activo = true
              AND d.usuario.id = :usuarioId
            """)
    long countByUsuarioId(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // BÚSQUEDA
    // ============================================================

    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
              AND LOWER(d.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY d.fechaCreacion DESC
            """)
    List<Documento> searchByTitulo(@Param("query") String query);

    /**
     * ✅ NUEVO: Búsqueda avanzada (título + descripción).
     */
    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
              AND (
                LOWER(d.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY d.fechaCreacion DESC
            """)
    Page<Documento> buscarAvanzado(
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
              AND d.categoria = :categoria
              AND LOWER(d.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY d.fechaCreacion DESC
            """)
    List<Documento> searchByCategoriaAndTitulo(
            @Param("categoria") String categoria,
            @Param("query") String query
    );

    /**
     * ✅ NUEVO: Búsqueda por autor (nombre de usuario).
     */
    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario u
            WHERE d.activo = true
              AND LOWER(u.nombreUsuario) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY d.fechaCreacion DESC
            """)
    Page<Documento> buscarPorAutor(
            @Param("query") String query,
            Pageable pageable
    );

    // ============================================================
    // OBTENER POR ID
    // ============================================================

    /**
     * ✅ NUEVO: Obtiene un documento activo con su autor cargado.
     */
    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.id = :id
              AND d.activo = true
            """)
    Optional<Documento> findActivoById(@Param("id") Long id);

    // ============================================================
    // TOP DOCUMENTOS (home, panel admin)
    // ============================================================

    /**
     * ✅ NUEVO: Top N documentos más descargados.
     */
    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
            ORDER BY d.descargas DESC, d.fechaCreacion DESC
            """)
    List<Documento> findTopMasDescargados(Pageable pageable);

    /**
     * ✅ NUEVO: Top N documentos más vistos.
     */
    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
            ORDER BY d.visitas DESC, d.fechaCreacion DESC
            """)
    List<Documento> findTopMasVistos(Pageable pageable);

    /**
     * ✅ NUEVO: Documentos recientes (para home).
     */
    @Query("""
            SELECT d FROM Documento d
            LEFT JOIN FETCH d.usuario
            WHERE d.activo = true
            ORDER BY d.fechaCreacion DESC
            """)
    List<Documento> findRecientes(Pageable pageable);

    // ============================================================
    // CONTADORES AGRUPADOS (para el feed — elimina N+1)
    // ============================================================

    /**
     * ✅ NUEVO: Cuenta documentos agrupados por categoría.
     * Devuelve [categoria, count].
     *
     * Útil para mostrar el número de documentos por filtro sin N+1.
     */
    @Query("""
            SELECT d.categoria, COUNT(d)
            FROM Documento d
            WHERE d.activo = true
            GROUP BY d.categoria
            """)
    List<Object[]> contarDocumentosPorCategoria();

    /**
     * ✅ NUEVO: Cuenta documentos agrupados por usuario.
     * Devuelve [usuarioId, count].
     */
    @Query("""
            SELECT d.usuario.id, COUNT(d)
            FROM Documento d
            WHERE d.activo = true
              AND d.usuario.id IN :usuarioIds
            GROUP BY d.usuario.id
            """)
    List<Object[]> contarDocumentosPorUsuarios(@Param("usuarioIds") List<Long> usuarioIds);

    // ============================================================
    // ACTUALIZACIONES BULK
    // ============================================================

    /**
     * ✅ NUEVO: Incrementa el contador de descargas en 1 (bulk).
     */
    @Modifying
    @Query("""
            UPDATE Documento d
            SET d.descargas = d.descargas + 1
            WHERE d.id = :id
            """)
    int incrementarDescargas(@Param("id") Long id);

    /**
     * ✅ NUEVO: Incrementa el contador de visitas en 1 (bulk).
     */
    @Modifying
    @Query("""
            UPDATE Documento d
            SET d.visitas = d.visitas + 1
            WHERE d.id = :id
            """)
    int incrementarVisitas(@Param("id") Long id);

    // ============================================================
    // VALIDACIONES
    // ============================================================

    /**
     * ✅ NUEVO: Verifica si existe un documento activo con el mismo título
     * para el mismo usuario (anti-duplicados).
     */
    @Query("""
            SELECT COUNT(d) > 0 FROM Documento d
            WHERE d.activo = true
              AND d.usuario.id = :usuarioId
              AND LOWER(d.titulo) = LOWER(:titulo)
            """)
    boolean existePorUsuarioYTitulo(
            @Param("usuarioId") Long usuarioId,
            @Param("titulo") String titulo
    );

    // ============================================================
    // ESTADÍSTICAS PARA ADMIN
    // ============================================================

    /**
     * ✅ NUEVO: Total de documentos activos.
     */
    @Query("SELECT COUNT(d) FROM Documento d WHERE d.activo = true")
    long countActivos();

    /**
     * ✅ NUEVO: Total de descargas en todos los documentos.
     */
    @Query("SELECT COALESCE(SUM(d.descargas), 0) FROM Documento d WHERE d.activo = true")
    long totalDescargas();

    /**
     * ✅ NUEVO: Total de visitas en todos los documentos.
     */
    @Query("SELECT COALESCE(SUM(d.visitas), 0) FROM Documento d WHERE d.activo = true")
    long totalVisitas();

    /**
     * ✅ NUEVO: Documentos creados en un rango de fechas (para reportes).
     */
    @Query("""
            SELECT d FROM Documento d
            WHERE d.activo = true
              AND d.fechaCreacion >= :desde
              AND d.fechaCreacion < :hasta
            ORDER BY d.fechaCreacion DESC
            """)
    List<Documento> findEnRangoDeFechas(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );
}