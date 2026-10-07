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

    // ============================================================
    // COMPRAS POR USUARIO
    // ============================================================

    /**
     * Lista todas las compras de un usuario, ordenadas por fecha DESC.
     * ⚠️ Mejor usar `findByUsuarioIdPaginado` para muchos registros.
     */
    @Query("""
            SELECT c FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
            ORDER BY c.fechaCompra DESC
            """)
    List<CompraUsuario> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * NUEVO: Versión paginada.
     */
    @Query("""
            SELECT c FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
            ORDER BY c.fechaCompra DESC
            """)
    Page<CompraUsuario> findByUsuarioIdPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    /**
     *  NUEVO: Compras por tipo (MARCOS, FONDOS, TEMAS, etc).
     */
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

    /**
     * NUEVO: IDs de items comprados por tipo (para el catálogo).
     * Uso típico: saber qué marcos ya tiene el usuario para marcarlos como "comprados".
     */
    @Query("""
            SELECT c.itemId FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
              AND c.tipo = :tipo
            """)
    List<String> findItemIdsComprados(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") String tipo
    );

    // ============================================================
    // VERIFICAR COMPRA ESPECÍFICA
    // ============================================================

    /**
     * Verifica si un usuario ya compró un item específico.
     * Uso: antes de permitir comprar de nuevo, o para marcar como "comprado" en el catálogo.
     */
    boolean existsByUsuarioIdAndTipoAndItemId(Long usuarioId, String tipo, String itemId);

    /**
     * NUEVO: Devuelve la compra específica si existe.
     * Uso: para mostrar detalles de la compra, o para auditoría.
     */
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

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     *  NUEVO: Total gastado por un usuario.
     */
    @Query("""
            SELECT COALESCE(SUM(c.precio), 0)
            FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
            """)
    BigDecimal totalGastadoPorUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * NUEVO: Total gastado agrupado por tipo.
     * Devuelve [tipo, total].
     */
    @Query("""
            SELECT c.tipo, COALESCE(SUM(c.precio), 0)
            FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
            GROUP BY c.tipo
            """)
    List<Object[]> totalGastadoPorTipo(@Param("usuarioId") Long usuarioId);

    /**
     * NUEVO: Cuántas compras tiene un usuario.
     */
    @Query("""
            SELECT COUNT(c) FROM CompraUsuario c
            WHERE c.usuario.id = :usuarioId
            """)
    long contarComprasDeUsuario(@Param("usuarioId") Long usuarioId);

    /**
     *  NUEVO: Cuántas veces se ha comprado un item específico.
     * Útil para saber qué marcos/fondos son más populares.
     */
    @Query("""
            SELECT COUNT(c) FROM CompraUsuario c
            WHERE c.tipo = :tipo
              AND c.itemId = :itemId
            """)
    long contarComprasDeItem(
            @Param("tipo") String tipo,
            @Param("itemId") String itemId
    );

    // ============================================================
    // TOP ITEMS MÁS COMPRADOS (para el catálogo)
    // ============================================================

    /**
     * NUEVO: Top N items más comprados por tipo.
     * Devuelve [itemId, count].
     */
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

    // ============================================================
    // HISTORIAL Y AUDITORÍA
    // ============================================================

    /**
     *  NUEVO: Compras recientes en un rango de fechas.
     * Útil para reportes admin.
     */
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

    /**
     *  NUEVO: Usuarios que compraron un item específico.
     */
    @Query("""
            SELECT c.usuario.id FROM CompraUsuario c
            WHERE c.tipo = :tipo
              AND c.itemId = :itemId
            """)
    List<Long> findUsuariosQueCompraron(
            @Param("tipo") String tipo,
            @Param("itemId") String itemId
    );

    // ============================================================
    // VALIDACIONES DE NEGOCIO
    // ============================================================

    /**
     *  NUEVO: Verifica si un usuario puede comprar un item.
     * Devuelve true si NO lo ha comprado antes (puede comprar).
     */
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
}