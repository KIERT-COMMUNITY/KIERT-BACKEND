// src/main/java/com/kiert/backend/repository/AnuncioRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Anuncio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {

    // ============================================================
    // LISTADOS BÁSICOS
    // ============================================================

    /**
     * Lista todos los anuncios activos (para la vista pública simple).
     * Ordenados por fecha DESC. Sin filtro de expiración.
     */
    List<Anuncio> findByActivoTrueOrderByFechaCreacionDesc();

    /**
     * Lista todos los anuncios (activos e inactivos) ordenados por fecha DESC.
     * Uso: panel de administración.
     */
    List<Anuncio> findAllByOrderByFechaCreacionDesc();

    /**
     * Versión paginada para el panel admin (recomendado si hay muchos).
     */
    @Query("""
            SELECT a FROM Anuncio a
            ORDER BY a.fechaCreacion DESC
            """)
    Page<Anuncio> findAllPaginado(Pageable pageable);

    /**
     * Versión paginada solo de activos.
     */
    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.activo = true
            ORDER BY a.destacado DESC, a.fechaCreacion DESC
            """)
    Page<Anuncio> findActivosPaginado(Pageable pageable);

    // ============================================================
    // VISTA PÚBLICA (usuarios normales)
    // ============================================================

    /**
     * ✅ Anuncios visibles para usuarios:
     *   - activo = true
     *   - sin fecha de expiración O fecha_expiracion > ahora
     * Ordenados por destacados primero, luego fecha DESC.
     */
    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.activo = true
              AND (a.fechaExpiracion IS NULL OR a.fechaExpiracion > :ahora)
            ORDER BY a.destacado DESC, a.fechaCreacion DESC
            """)
    List<Anuncio> findActivosVigentes(@Param("ahora") LocalDateTime ahora);

    /**
     * Versión paginada de los vigentes (para scroll infinito / paginación).
     */
    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.activo = true
              AND (a.fechaExpiracion IS NULL OR a.fechaExpiracion > :ahora)
            ORDER BY a.destacado DESC, a.fechaCreacion DESC
            """)
    Page<Anuncio> findActivosVigentesPaginado(
            @Param("ahora") LocalDateTime ahora,
            Pageable pageable
    );

    /**
     * Solo los destacados vigentes (para un carrusel superior, por ejemplo).
     */
    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.activo = true
              AND a.destacado = true
              AND (a.fechaExpiracion IS NULL OR a.fechaExpiracion > :ahora)
            ORDER BY a.fechaCreacion DESC
            """)
    List<Anuncio> findDestacadosVigentes(@Param("ahora") LocalDateTime ahora);

    // ============================================================
    // BÚSQUEDAS POR ID / ESTADO
    // ============================================================

    /**
     * Busca un anuncio por ID solo si está activo (para vista pública).
     */
    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.id = :id
              AND a.activo = true
            """)
    Optional<Anuncio> findActivoPorId(@Param("id") Long id);

    /**
     * Anuncios creados por un admin específico (auditoría).
     */
    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.creadoPorId = :adminId
            ORDER BY a.fechaCreacion DESC
            """)
    List<Anuncio> findByCreadoPorId(@Param("adminId") Long adminId);

    /**
     * Anuncios actualizados por un admin específico (auditoría).
     */
    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.actualizadoPorId = :adminId
            ORDER BY a.fechaActualizacion DESC
            """)
    List<Anuncio> findByActualizadoPorId(@Param("adminId") Long adminId);

    // ============================================================
    // EXPIRACIÓN
    // ============================================================

    /**
     * ✅ Anuncios ya expirados que siguen activos.
     * Uso: job programado que los desactive automáticamente.
     */
    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.activo = true
              AND a.fechaExpiracion IS NOT NULL
              AND a.fechaExpiracion <= :ahora
            """)
    List<Anuncio> findExpiradosActivos(@Param("ahora") LocalDateTime ahora);

    /**
     * Desactiva en bloque todos los anuncios expirados.
     * Devuelve cuántos se actualizaron.
     */
    @Modifying
    @Query("""
            UPDATE Anuncio a
            SET a.activo = false,
                a.fechaActualizacion = :ahora
            WHERE a.activo = true
              AND a.fechaExpiracion IS NOT NULL
              AND a.fechaExpiracion <= :ahora
            """)
    int desactivarExpirados(@Param("ahora") LocalDateTime ahora);

    /**
     * Cuenta cuántos están expirados pero activos (para dashboard admin).
     */
    @Query("""
            SELECT COUNT(a) FROM Anuncio a
            WHERE a.activo = true
              AND a.fechaExpiracion IS NOT NULL
              AND a.fechaExpiracion <= :ahora
            """)
    long countExpiradosActivos(@Param("ahora") LocalDateTime ahora);

    // ============================================================
    // ESTADÍSTICAS (dashboard admin)
    // ============================================================

    /**
     * Total de anuncios activos.
     */
    long countByActivoTrue();

    /**
     * Total de anuncios inactivos.
     */
    long countByActivoFalse();

    /**
     * Total de anuncios destacados y activos.
     */
    @Query("""
            SELECT COUNT(a) FROM Anuncio a
            WHERE a.activo = true
              AND a.destacado = true
            """)
    long countDestacadosActivos();

    /**
     * Total de anuncios vigentes (activos y no expirados).
     */
    @Query("""
            SELECT COUNT(a) FROM Anuncio a
            WHERE a.activo = true
              AND (a.fechaExpiracion IS NULL OR a.fechaExpiracion > :ahora)
            """)
    long countVigentes(@Param("ahora") LocalDateTime ahora);

    /**
     * Total de anuncios creados en un rango de fechas.
     */
    @Query("""
            SELECT COUNT(a) FROM Anuncio a
            WHERE a.fechaCreacion BETWEEN :desde AND :hasta
            """)
    long countCreadosEnRango(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    // ============================================================
    // VALIDACIONES DE NEGOCIO
    // ============================================================

    /**
     * ¿Existe un anuncio con este título? (para evitar duplicados)
     */
    boolean existsByTituloIgnoreCase(String titulo);

    /**
     * ¿Existe otro anuncio con este título distinto al ID dado?
     * Uso: al actualizar, para permitir mantener el mismo título.
     */
    @Query("""
            SELECT COUNT(a) > 0 FROM Anuncio a
            WHERE LOWER(a.titulo) = LOWER(:titulo)
              AND a.id <> :idExcluir
            """)
    boolean existsByTituloIgnoreCaseAndIdNot(
            @Param("titulo") String titulo,
            @Param("idExcluir") Long idExcluir
    );

    // ============================================================
    // MANTENIMIENTO
    // ============================================================

    /**
     * Elimina anuncios inactivos creados antes de una fecha.
     * Uso: limpieza de anuncios viejos.
     */
    @Modifying
    @Query("""
            DELETE FROM Anuncio a
            WHERE a.activo = false
              AND a.fechaCreacion < :antesDe
            """)
    int eliminarInactivosAnterioresA(@Param("antesDe") LocalDateTime antesDe);
}