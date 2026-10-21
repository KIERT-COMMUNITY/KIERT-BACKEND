// src/main/java/com/kiert/backend/repository/CompraRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.CompraUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<CompraUsuario, Long> {

    @Query("""
            SELECT c FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
            ORDER BY c.fechaCompra DESC
            """)
    List<CompraUsuario> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT c FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
            ORDER BY c.fechaCompra DESC
            """)
    Page<CompraUsuario> findByUsuarioIdPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Query("""
            SELECT c FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
              AND c.tipo = :tipo
            ORDER BY c.fechaCompra DESC
            """)
    List<CompraUsuario> findByUsuarioIdAndTipo(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") String tipo
    );

    @Query("""
            SELECT c.itemId FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
              AND c.tipo = :tipo
            """)
    List<String> findItemIdsComprados(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") String tipo
    );

    boolean existsByUsuarioIdAndTipoAndItemId(Long usuarioId, String tipo, String itemId);

    @Query("""
            SELECT c FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
              AND c.tipo = :tipo
              AND c.itemId = :itemId
            ORDER BY c.fechaCompra DESC
            """)
    Optional<CompraUsuario> findByUsuarioIdAndTipoAndItemId(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") String tipo,
            @Param("itemId") String itemId
    );

    @Query("""
            SELECT COALESCE(SUM(c.precio), 0)
            FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
            """)
    BigDecimal totalGastadoPorUsuario(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT c.tipo, COALESCE(SUM(c.precio), 0)
            FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
            GROUP BY c.tipo
            """)
    List<Object[]> totalGastadoPorTipo(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT COUNT(c) FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
            """)
    long contarComprasDeUsuario(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT COUNT(c) FROM CompraUsuario c
            WHERE c.tipo = :tipo
              AND c.itemId = :itemId
            """)
    long contarComprasDeItem(
            @Param("tipo") String tipo,
            @Param("itemId") String itemId
    );

    @Query("""
            SELECT c.itemId, COUNT(c) as total
            FROM CompraUsuario c
            WHERE c.tipo = :tipo
            GROUP BY c.itemId
            ORDER BY total DESC
            """)
    List<Object[]> topItemsMasComprados(
            @Param("tipo") String tipo,
            Pageable pageable
    );

    @Query("""
            SELECT c FROM CompraUsuario c
            WHERE c.fechaCompra >= :desde
              AND c.fechaCompra < :hasta
            ORDER BY c.fechaCompra DESC
            """)
    List<CompraUsuario> findComprasEnRango(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );

    @Query("""
            SELECT c.usuario.id FROM CompraUsuario c
            WHERE c.tipo = :tipo
              AND c.itemId = :itemId
            """)
    List<Long> findUsuariosQueCompraron(
            @Param("tipo") String tipo,
            @Param("itemId") String itemId
    );

    @Query("""
            SELECT COUNT(c) = 0 FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
              AND c.tipo = :tipo
              AND c.itemId = :itemId
            """)
    boolean puedeComprar(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") String tipo,
            @Param("itemId") String itemId
    );

    // ============================================================
    // SEGURIDAD: Verificar que el usuario es dueño de la compra
    // ============================================================

    /**
     * NUEVO: Verifica si el usuario es dueño de la compra.
     * Se usa para impedir que un usuario vea o elimine compras ajenas.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM CompraUsuario c
            WHERE c.id = :compraId
              AND c.usuario.id = :usuarioId
            """)
    boolean esCompraDelUsuario(
            @Param("compraId") Long compraId,
            @Param("usuarioId") Long usuarioId
    );
}