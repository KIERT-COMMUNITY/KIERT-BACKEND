// src/main/java/com/kiert/backend/repository/FondoRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.FondoPersonalizado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FondoRepository extends JpaRepository<FondoPersonalizado, Long> {

    // ============================================================
    // LISTAR FONDOS
    // ============================================================

    /**
     * Lista todos los fondos activos ordenados por fecha DESC.
     * ⚠️ Para catálogos grandes, usa `findByActivoTruePaginado`.
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findByActivoTrue();

    /**
     * ✅ NUEVO: Versión paginada del catálogo.
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
            ORDER BY f.fechaCreacion DESC
            """)
    Page<FondoPersonalizado> findByActivoTruePaginado(Pageable pageable);

    // ============================================================
    // FILTRAR POR GRATIS / PAGO
    // ============================================================

    /**
     * ✅ NUEVO: Solo fondos gratuitos (para usuarios sin premium).
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND f.gratis = true
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findGratuitos();

    /**
     * ✅ NUEVO: Solo fondos de pago (para el catálogo premium).
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND f.gratis = false
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findDePago();

    // ============================================================
    // FILTRAR POR TIPO
    // ============================================================

    /**
     * ✅ NUEVO: Fondos por tipo (gradiente, imagen, video, patrón).
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND f.tipo = :tipo
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findByTipo(@Param("tipo") String tipo);

    /**
     * ✅ NUEVO: Fondos por tipo + gratis (para filtro combinado).
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND f.tipo = :tipo
              AND f.gratis = :gratis
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findByTipoAndGratis(
            @Param("tipo") String tipo,
            @Param("gratis") Boolean gratis
    );

    // ============================================================
    // FILTRAR POR PRECIO
    // ============================================================

    /**
     * ✅ NUEVO: Fondos en rango de precio (para filtros de tienda).
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND f.gratis = false
              AND f.precio BETWEEN :min AND :max
            ORDER BY f.precio ASC
            """)
    List<FondoPersonalizado> findByPrecioEntre(
            @Param("min") BigDecimal min,
            @Param("max") BigDecimal max
    );

    // ============================================================
    // OBTENER POR ID
    // ============================================================

    /**
     * ✅ NUEVO: Obtiene un fondo activo por ID.
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.id = :id
              AND f.activo = true
            """)
    Optional<FondoPersonalizado> findActivoById(@Param("id") Long id);

    /**
     * ✅ NUEVO: Obtiene un fondo activo por nombre (case-insensitive).
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND LOWER(f.nombre) = LOWER(:nombre)
            """)
    Optional<FondoPersonalizado> findActivoByNombre(@Param("nombre") String nombre);

    // ============================================================
    // BÚSQUEDA
    // ============================================================

    /**
     * ✅ NUEVO: Búsqueda por nombre o descripción.
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND (
                LOWER(f.nombre) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(f.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY f.fechaCreacion DESC
            """)
    Page<FondoPersonalizado> buscar(
            @Param("query") String query,
            Pageable pageable
    );

    // ============================================================
    // TOP FONDOS (home, recomendaciones)
    // ============================================================

    /**
     * ✅ NUEVO: Top N fondos más recientes.
     * Útil para el home.
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findRecientes(Pageable pageable);

    /**
     * ✅ NUEVO: Top N fondos gratuitos (para nuevos usuarios).
     */
    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND f.gratis = true
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findGratuitosRecientes(Pageable pageable);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * ✅ NUEVO: Cuenta fondos activos.
     */
    @Query("SELECT COUNT(f) FROM FondoPersonalizado f WHERE f.activo = true")
    long countActivos();

    /**
     * ✅ NUEVO: Cuenta fondos agrupados por tipo.
     * Devuelve [tipo, count].
     */
    @Query("""
            SELECT f.tipo, COUNT(f)
            FROM FondoPersonalizado f
            WHERE f.activo = true
            GROUP BY f.tipo
            """)
    List<Object[]> contarPorTipo();

    /**
     * ✅ NUEVO: Cuenta fondos agrupados por gratis/pago.
     * Devuelve [gratis, count].
     */
    @Query("""
            SELECT f.gratis, COUNT(f)
            FROM FondoPersonalizado f
            WHERE f.activo = true
            GROUP BY f.gratis
            """)
    List<Object[]> contarPorGratis();

    /**
     * ✅ NUEVO: Tipos distintos disponibles (para filtros del frontend).
     */
    @Query("""
            SELECT DISTINCT f.tipo FROM FondoPersonalizado f
            WHERE f.activo = true
            ORDER BY f.tipo ASC
            """)
    List<String> findDistinctTipos();

    // ============================================================
    // VALIDACIONES
    // ============================================================

    /**
     * ✅ NUEVO: Verifica si existe un fondo activo con ese nombre.
     */
    @Query("""
            SELECT COUNT(f) > 0 FROM FondoPersonalizado f
            WHERE f.activo = true
              AND LOWER(f.nombre) = LOWER(:nombre)
            """)
    boolean existePorNombre(@Param("nombre") String nombre);

    // ============================================================
    // ADMIN: DESACTIVAR / ACTIVAR EN LOTE
    // ============================================================

    /**
     * ✅ NUEVO: Desactiva varios fondos en 1 query (bulk).
     * Útil para el panel admin.
     */
    @Modifying
    @Query("""
            UPDATE FondoPersonalizado f
            SET f.activo = false
            WHERE f.id IN :ids
            """)
    int desactivarEnLote(@Param("ids") List<Long> ids);

    /**
     * ✅ NUEVO: Activa varios fondos en 1 query (bulk).
     */
    @Modifying
    @Query("""
            UPDATE FondoPersonalizado f
            SET f.activo = true
            WHERE f.id IN :ids
            """)
    int activarEnLote(@Param("ids") List<Long> ids);
}