// src/main/java/com/kiert/backend/repository/MarcoRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.MarcoPersonalizado;
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
public interface MarcoRepository extends JpaRepository<MarcoPersonalizado, Long> {

    // ============================================================
    // LISTAR MARCOS
    // ============================================================

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.activo = true
            ORDER BY m.fechaCreacion DESC
            """)
    List<MarcoPersonalizado> findByActivoTrue();

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.activo = true
            ORDER BY m.fechaCreacion DESC
            """)
    Page<MarcoPersonalizado> findByActivoTruePaginado(Pageable pageable);

    // ============================================================
    // FILTRAR POR GRATIS / PAGO
    // ============================================================

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.activo = true
              AND m.gratis = true
            ORDER BY m.fechaCreacion DESC
            """)
    List<MarcoPersonalizado> findGratuitos();

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.activo = true
              AND m.gratis = false
            ORDER BY m.fechaCreacion DESC
            """)
    List<MarcoPersonalizado> findDePago();

    // ============================================================
    // FILTRAR POR TIPO
    // ============================================================

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.activo = true
              AND m.tipo = :tipo
            ORDER BY m.fechaCreacion DESC
            """)
    List<MarcoPersonalizado> findByTipo(@Param("tipo") String tipo);

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.activo = true
              AND m.tipo = :tipo
              AND m.gratis = :gratis
            ORDER BY m.fechaCreacion DESC
            """)
    List<MarcoPersonalizado> findByTipoAndGratis(
            @Param("tipo") String tipo,
            @Param("gratis") Boolean gratis
    );

    // ============================================================
    // FILTRAR POR PRECIO
    // ============================================================

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.activo = true
              AND m.gratis = false
              AND m.precio BETWEEN :min AND :max
            ORDER BY m.precio ASC
            """)
    List<MarcoPersonalizado> findByPrecioEntre(
            @Param("min") BigDecimal min,
            @Param("max") BigDecimal max
    );

    // ============================================================
    // MARCOS POR USUARIO (CORREGIDO: usa m.usuario.id)
    // ============================================================

    /**
     * Lista marcos creados por un usuario específico.
     * Usa `m.usuario.id` en lugar de `m.usuarioId`.
     */
    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.usuario.id = :usuarioId
            ORDER BY m.fechaCreacion DESC
            """)
    List<MarcoPersonalizado> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Marcos activos de un usuario.
     */
    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.usuario.id = :usuarioId
              AND m.activo = true
            ORDER BY m.fechaCreacion DESC
            """)
    List<MarcoPersonalizado> findActivosByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Cuenta marcos creados por un usuario.
     */
    @Query("""
            SELECT COUNT(m) FROM MarcoPersonalizado m
            WHERE m.usuario.id = :usuarioId
            """)
    long countByUsuarioId(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // OBTENER POR ID
    // ============================================================

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.id = :id
              AND m.activo = true
            """)
    Optional<MarcoPersonalizado> findActivoById(@Param("id") Long id);

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.activo = true
              AND m.nombre = :nombre
            """)
    Optional<MarcoPersonalizado> findActivoByNombre(@Param("nombre") String nombre);

    // ============================================================
    // BÚSQUEDA
    // ============================================================

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.activo = true
              AND (
                LOWER(m.nombre) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(m.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY m.fechaCreacion DESC
            """)
    Page<MarcoPersonalizado> buscar(
            @Param("query") String query,
            Pageable pageable
    );

    // ============================================================
    // TOP MARCOS
    // ============================================================

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.activo = true
            ORDER BY m.fechaCreacion DESC
            """)
    List<MarcoPersonalizado> findRecientes(Pageable pageable);

    @Query("""
            SELECT m FROM MarcoPersonalizado m
            WHERE m.activo = true
              AND m.gratis = true
            ORDER BY m.fechaCreacion DESC
            """)
    List<MarcoPersonalizado> findGratuitosRecientes(Pageable pageable);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    @Query("SELECT COUNT(m) FROM MarcoPersonalizado m WHERE m.activo = true")
    long countActivos();

    @Query("""
            SELECT m.tipo, COUNT(m)
            FROM MarcoPersonalizado m
            WHERE m.activo = true
            GROUP BY m.tipo
            """)
    List<Object[]> contarPorTipo();

    @Query("""
            SELECT m.gratis, COUNT(m)
            FROM MarcoPersonalizado m
            WHERE m.activo = true
            GROUP BY m.gratis
            """)
    List<Object[]> contarPorGratis();

    @Query("""
            SELECT DISTINCT m.tipo FROM MarcoPersonalizado m
            WHERE m.activo = true
            ORDER BY m.tipo ASC
            """)
    List<String> findDistinctTipos();

    // ============================================================
    // VALIDACIONES
    // ============================================================

    @Query("""
            SELECT COUNT(m) > 0 FROM MarcoPersonalizado m
            WHERE m.activo = true
              AND m.nombre = :nombre
            """)
    boolean existePorNombre(@Param("nombre") String nombre);

    // ============================================================
    // ADMIN: DESACTIVAR / ACTIVAR EN LOTE
    // ============================================================

    @Modifying
    @Query("""
            UPDATE MarcoPersonalizado m
            SET m.activo = false
            WHERE m.id IN :ids
            """)
    int desactivarEnLote(@Param("ids") List<Long> ids);

    @Modifying
    @Query("""
            UPDATE MarcoPersonalizado m
            SET m.activo = true
            WHERE m.id IN :ids
            """)
    int activarEnLote(@Param("ids") List<Long> ids);
}