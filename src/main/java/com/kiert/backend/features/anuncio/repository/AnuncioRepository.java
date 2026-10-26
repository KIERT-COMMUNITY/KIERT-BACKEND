// src/main/java/com/kiert/backend/repository/AnuncioRepository.java
package com.kiert.backend.features.anuncio.repository;

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

    List<Anuncio> findByActivoTrueOrderByFechaCreacionDesc();

    List<Anuncio> findAllByOrderByFechaCreacionDesc();

    @Query("""
            SELECT a FROM Anuncio a
            ORDER BY a.fechaCreacion DESC
            """)
    Page<Anuncio> findAllPaginado(Pageable pageable);

    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.activo = true
            ORDER BY a.destacado DESC, a.fechaCreacion DESC
            """)
    Page<Anuncio> findActivosPaginado(Pageable pageable);

    // ============================================================
    // VISTA PÚBLICA (filtrado estricto)
    // ============================================================

    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.activo = true
              AND (a.fechaExpiracion IS NULL OR a.fechaExpiracion > :ahora)
            ORDER BY a.destacado DESC, a.fechaCreacion DESC
            """)
    List<Anuncio> findActivosVigentes(@Param("ahora") LocalDateTime ahora);

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

    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.activo = true
              AND a.destacado = true
              AND (a.fechaExpiracion IS NULL OR a.fechaExpiracion > :ahora)
            ORDER BY a.fechaCreacion DESC
            """)
    List<Anuncio> findDestacadosVigentes(@Param("ahora") LocalDateTime ahora);

    // ============================================================
    // BANNER FLOTANTE
    // ============================================================
    Optional<Anuncio> findFirstByEsFlotanteTrueAndActivoTrueOrderByFechaCreacionDesc();

    // ============================================================
    // BÚSQUEDAS POR ID / ESTADO
    // ============================================================

    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.id = :id
              AND a.activo = true
            """)
    Optional<Anuncio> findActivoPorId(@Param("id") Long id);

    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.creadoPorId = :adminId
            ORDER BY a.fechaCreacion DESC
            """)
    List<Anuncio> findByCreadoPorId(@Param("adminId") Long adminId);

    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.actualizadoPorId = :adminId
            ORDER BY a.fechaActualizacion DESC
            """)
    List<Anuncio> findByActualizadoPorId(@Param("adminId") Long adminId);

    // ============================================================
    // EXPIRACIÓN
    // ============================================================

    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.activo = true
              AND a.fechaExpiracion IS NOT NULL
              AND a.fechaExpiracion <= :ahora
            """)
    List<Anuncio> findExpiradosActivos(@Param("ahora") LocalDateTime ahora);

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

    @Query("""
            SELECT COUNT(a) FROM Anuncio a
            WHERE a.activo = true
              AND a.fechaExpiracion IS NOT NULL
              AND a.fechaExpiracion <= :ahora
            """)
    long countExpiradosActivos(@Param("ahora") LocalDateTime ahora);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    long countByActivoTrue();
    long countByActivoFalse();

    @Query("""
            SELECT COUNT(a) FROM Anuncio a
            WHERE a.activo = true
              AND a.destacado = true
            """)
    long countDestacadosActivos();

    @Query("""
            SELECT COUNT(a) FROM Anuncio a
            WHERE a.activo = true
              AND (a.fechaExpiracion IS NULL OR a.fechaExpiracion > :ahora)
            """)
    long countVigentes(@Param("ahora") LocalDateTime ahora);

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

    boolean existsByTituloIgnoreCase(String titulo);

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

    @Modifying
    @Query("""
            DELETE FROM Anuncio a
            WHERE a.activo = false
              AND a.fechaCreacion < :antesDe
            """)
    int eliminarInactivosAnterioresA(@Param("antesDe") LocalDateTime antesDe);
}