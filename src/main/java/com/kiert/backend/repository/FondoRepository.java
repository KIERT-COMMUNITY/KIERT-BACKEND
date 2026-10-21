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

    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findByActivoTrue();

    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
            ORDER BY f.fechaCreacion DESC
            """)
    Page<FondoPersonalizado> findByActivoTruePaginado(Pageable pageable);

    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND f.gratis = true
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findGratuitos();

    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND f.gratis = false
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findDePago();

    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND f.tipo = :tipo
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findByTipo(@Param("tipo") String tipo);

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

    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.id = :id
              AND f.activo = true
            """)
    Optional<FondoPersonalizado> findActivoById(@Param("id") Long id);

    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND LOWER(f.nombre) = LOWER(:nombre)
            """)
    Optional<FondoPersonalizado> findActivoByNombre(@Param("nombre") String nombre);

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

    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findRecientes(Pageable pageable);

    @Query("""
            SELECT f FROM FondoPersonalizado f
            WHERE f.activo = true
              AND f.gratis = true
            ORDER BY f.fechaCreacion DESC
            """)
    List<FondoPersonalizado> findGratuitosRecientes(Pageable pageable);

    @Query("SELECT COUNT(f) FROM FondoPersonalizado f WHERE f.activo = true")
    long countActivos();

    @Query("""
            SELECT f.tipo, COUNT(f)
            FROM FondoPersonalizado f
            WHERE f.activo = true
            GROUP BY f.tipo
            """)
    List<Object[]> contarPorTipo();

    @Query("""
            SELECT f.gratis, COUNT(f)
            FROM FondoPersonalizado f
            WHERE f.activo = true
            GROUP BY f.gratis
            """)
    List<Object[]> contarPorGratis();

    @Query("""
            SELECT DISTINCT f.tipo FROM FondoPersonalizado f
            WHERE f.activo = true
            ORDER BY f.tipo ASC
            """)
    List<String> findDistinctTipos();

    @Query("""
            SELECT COUNT(f) > 0 FROM FondoPersonalizado f
            WHERE f.activo = true
              AND LOWER(f.nombre) = LOWER(:nombre)
            """)
    boolean existePorNombre(@Param("nombre") String nombre);

    @Modifying
    @Query("""
            UPDATE FondoPersonalizado f
            SET f.activo = false
            WHERE f.id IN :ids
            """)
    int desactivarEnLote(@Param("ids") List<Long> ids);

    @Modifying
    @Query("""
            UPDATE FondoPersonalizado f
            SET f.activo = true
            WHERE f.id IN :ids
            """)
    int activarEnLote(@Param("ids") List<Long> ids);
}