// src/main/java/com/kiert/backend/repository/CodigoVerificacionRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.CodigoVerificacion;
import com.kiert.backend.entity.TipoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    // ============================================================
    // BUSCAR CÓDIGO VIGENTE
    // ============================================================
    /**
     * Busca el código más reciente que coincida con email, código y tipo,
     * que NO esté usado y que NO esté expirado.
     *
     *  AÑADIDO: filtro `fechaExpiracion > NOW()` para evitar traer
     * códigos expirados. Ahora el service no necesita verificar la expiración
     * por separado.
     *
     * ⚠️ Requiere índice: idx_codigos_busqueda (email, codigo, tipo, usado, fecha_expiracion)
     */
    @Query("""
            SELECT c FROM CodigoVerificacion c
            WHERE c.email = :email
              AND c.codigo = :codigo
              AND c.tipo = :tipo
              AND c.usado = false
              AND c.fechaExpiracion > :ahora
            ORDER BY c.fechaCreacion DESC
            """)
    Optional<CodigoVerificacion> findCodigoVigente(
            @Param("email") String email,
            @Param("codigo") String codigo,
            @Param("tipo") TipoCodigo tipo,
            @Param("ahora") Instant ahora
    );

    /**
     * Método legacy (compatibilidad). Usa `findCodigoVigente` si puedes.
     */
    @Deprecated
    Optional<CodigoVerificacion> findTopByEmailAndCodigoAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
            String email, String codigo, TipoCodigo tipo);

    // ============================================================
    // INVALIDAR CÓDIGOS ANTERIORES
    // ============================================================
    /**
     * Marca como usados todos los códigos vigentes de un email+tipo.
     * Se llama antes de generar uno nuevo para evitar múltiples códigos activos.
     */
    @Modifying
    @Query("""
            UPDATE CodigoVerificacion c
            SET c.usado = true,
                c.fechaUso = :ahora
            WHERE c.email = :email
              AND c.tipo = :tipo
              AND c.usado = false
            """)
    int invalidarCodigosAnteriores(
            @Param("email") String email,
            @Param("tipo") TipoCodigo tipo,
            @Param("ahora") Instant ahora
    );

    /**
     * ⚠️ Legacy: mantiene la firma original sin `ahora`.
     * Se puede eliminar cuando actualices el service.
     */
    @Deprecated
    @Modifying
    @Query("""
            UPDATE CodigoVerificacion c
            SET c.usado = true
            WHERE c.email = :email
              AND c.tipo = :tipo
              AND c.usado = false
            """)
    void invalidarCodigosAnteriores(
            @Param("email") String email,
            @Param("tipo") TipoCodigo tipo
    );

    // ============================================================
    // INVALIDAR TODOS LOS CÓDIGOS DE UN EMAIL (todos los tipos)
    // ============================================================
    /**
     * NUEVO: Invalida TODOS los códigos vigentes del email,
     * independientemente del tipo. Útil cuando el usuario cambia de
     * email, resetea password, o cuando sospechas de abuso.
     */
    @Modifying
    @Query("""
            UPDATE CodigoVerificacion c
            SET c.usado = true,
                c.fechaUso = :ahora
            WHERE c.email = :email
              AND c.usado = false
            """)
    int invalidarTodosLosCodigos(
            @Param("email") String email,
            @Param("ahora") Instant ahora
    );

    // ============================================================
    // MARCAR UN CÓDIGO COMO USADO
    // ============================================================
    /**
     * NUEVO: Marca un código específico como usado.
     * Útil cuando ya tienes la entidad cargada y solo quieres consumirla.
     */
    @Modifying
    @Query("""
            UPDATE CodigoVerificacion c
            SET c.usado = true,
                c.fechaUso = :ahora
            WHERE c.id = :id
            """)
    int marcarComoUsado(
            @Param("id") Long id,
            @Param("ahora") Instant ahora
    );

    // ============================================================
    // CONTAR CÓDIGOS RECIENTES (rate limiting adicional)
    // ============================================================
    /**
     * NUEVO: Cuenta cuántos códigos se han generado para un email
     * en las últimas N horas. Se usa como rate limiting adicional a
     * nivel de BD (complementa el rate limiting de Redis).
     */
    @Query("""
            SELECT COUNT(c) FROM CodigoVerificacion c
            WHERE c.email = :email
              AND c.tipo = :tipo
              AND c.fechaCreacion >= :desde
            """)
    long contarCodigosRecientes(
            @Param("email") String email,
            @Param("tipo") TipoCodigo tipo,
            @Param("desde") Instant desde
    );

    // ============================================================
    // BUSCAR ÚLTIMO CÓDIGO VIGENTE POR EMAIL
    // ============================================================
    /**
     * NUEVO: Obtiene el código vigente más reciente de un email+tipo.
     * Útil para debugging o para saber si hay un código activo.
     */
    @Query("""
            SELECT c FROM CodigoVerificacion c
            WHERE c.email = :email
              AND c.tipo = :tipo
              AND c.usado = false
              AND c.fechaExpiracion > :ahora
            ORDER BY c.fechaCreacion DESC
            """)
    List<CodigoVerificacion> findCodigosVigentes(
            @Param("email") String email,
            @Param("tipo") TipoCodigo tipo,
            @Param("ahora") Instant ahora
    );

    // ============================================================
    // LIMPIEZA (para @Scheduled job)
    // ============================================================
    /**
     * NUEVO: Elimina códigos expirados hace más de N días.
     * Se ejecuta con un @Scheduled cada noche para mantener la tabla limpia.
     *
     * ⚠️ Devuelve el número de filas eliminadas.
     */
    @Modifying
    @Transactional
    @Query("""
            DELETE FROM CodigoVerificacion c
            WHERE c.fechaExpiracion < :limite
            """)
    int eliminarCodigosExpirados(@Param("limite") Instant limite);

    // ============================================================
    // VERIFICAR SI EXISTE CÓDIGO VIGENTE
    // ============================================================
    /**
     *  NUEVO: Verifica si el usuario tiene algún código vigente.
     * Útil para evitar generar otro si ya hay uno activo.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM CodigoVerificacion c
            WHERE c.email = :email
              AND c.tipo = :tipo
              AND c.usado = false
              AND c.fechaExpiracion > :ahora
            """)
    boolean existeCodigoVigente(
            @Param("email") String email,
            @Param("tipo") TipoCodigo tipo,
            @Param("ahora") Instant ahora
    );
}