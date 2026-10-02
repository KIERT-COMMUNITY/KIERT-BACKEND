// src/main/java/com/kiert/backend/repository/GrupoHistorialRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.GrupoHistorial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface GrupoHistorialRepository extends JpaRepository<GrupoHistorial, Long> {

    // ============================================================
    // LISTAR HISTORIAL DE UN GRUPO
    // ============================================================

    /**
     * Lista el historial completo de un grupo ordenado por fecha DESC.
     * ✅ MEJORA: incluye JOIN FETCH del usuario para evitar N+1.
     * ⚠️ Para grupos con muchas acciones, usa `findByGrupoIdPaginado`.
     */
    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
            ORDER BY h.fecha DESC
            """)
    List<GrupoHistorial> findByGrupoIdOrderByFechaDesc(@Param("grupoId") Long grupoId);

    /**
     * ✅ NUEVO: Versión paginada (recomendada para grupos activos).
     */
    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
            ORDER BY h.fecha DESC
            """)
    Page<GrupoHistorial> findByGrupoIdPaginado(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    // ============================================================
    // FILTRAR POR ACCIÓN
    // ============================================================

    /**
     * ✅ NUEVO: Historial filtrado por tipo de acción.
     * Ej: "EXPULSAR", "INVITAR", "EDITAR_NOMBRE", etc.
     */
    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
              AND h.accion = :accion
            ORDER BY h.fecha DESC
            """)
    List<GrupoHistorial> findByGrupoIdAndAccion(
            @Param("grupoId") Long grupoId,
            @Param("accion") String accion
    );

    /**
     * ✅ NUEVO: Contar acciones por tipo en un grupo.
     * Devuelve [accion, count].
     */
    @Query("""
            SELECT h.accion, COUNT(h)
            FROM GrupoHistorial h
            WHERE h.grupo.id = :grupoId
            GROUP BY h.accion
            ORDER BY COUNT(h) DESC
            """)
    List<Object[]> contarAccionesPorTipo(@Param("grupoId") Long grupoId);

    // ============================================================
    // FILTRAR POR USUARIO
    // ============================================================

    /**
     * ✅ NUEVO: Historial de acciones hechas por un usuario específico en un grupo.
     */
    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
              AND h.usuario.id = :usuarioId
            ORDER BY h.fecha DESC
            """)
    List<GrupoHistorial> findByGrupoIdAndUsuarioId(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    /**
     * ✅ NUEVO: Todas las acciones hechas por un usuario (en todos los grupos).
     */
    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.grupo
            WHERE h.usuario.id = :usuarioId
            ORDER BY h.fecha DESC
            """)
    Page<GrupoHistorial> findByUsuarioId(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    // ============================================================
    // FILTRAR POR RANGO DE FECHAS
    // ============================================================

    /**
     * ✅ NUEVO: Historial de un grupo en un rango de fechas.
     * Útil para auditorías o reportes.
     */
    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
              AND h.fecha >= :desde
              AND h.fecha < :hasta
            ORDER BY h.fecha DESC
            """)
    List<GrupoHistorial> findByGrupoIdAndFechaEntre(
            @Param("grupoId") Long grupoId,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );

    /**
     * ✅ NUEVO: Últimas N acciones de un grupo (para el resumen rápido).
     */
    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
            ORDER BY h.fecha DESC
            """)
    List<GrupoHistorial> findUltimasAcciones(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    // ============================================================
    // BÚSQUEDA AVANZADA
    // ============================================================

    /**
     * ✅ NUEVO: Búsqueda por detalle (texto).
     * Ej: buscar "expulsó a juan" o "cambió el nombre".
     */
    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
              AND LOWER(h.detalle) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY h.fecha DESC
            """)
    Page<GrupoHistorial> buscarPorDetalle(
            @Param("grupoId") Long grupoId,
            @Param("query") String query,
            Pageable pageable
    );

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * ✅ NUEVO: Total de acciones registradas en un grupo.
     */
    @Query("""
            SELECT COUNT(h) FROM GrupoHistorial h
            WHERE h.grupo.id = :grupoId
            """)
    long countAccionesDeGrupo(@Param("grupoId") Long grupoId);

    /**
     * ✅ NUEVO: Total de acciones hechas por un usuario en un grupo.
     */
    @Query("""
            SELECT COUNT(h) FROM GrupoHistorial h
            WHERE h.grupo.id = :grupoId
              AND h.usuario.id = :usuarioId
            """)
    long countAccionesDeUsuarioEnGrupo(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    /**
     * ✅ NUEVO: Acciones más recientes en todos los grupos (para admin).
     */
    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            LEFT JOIN FETCH h.grupo
            ORDER BY h.fecha DESC
            """)
    Page<GrupoHistorial> findAccionesRecientes(Pageable pageable);

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * ✅ NUEVO: Elimina acciones antiguas (por ejemplo, > 1 año).
     * Se ejecuta con un @Scheduled para mantener la tabla pequeña.
     */
    @Modifying
    @Query("""
            DELETE FROM GrupoHistorial h
            WHERE h.fecha < :limite
            """)
    int eliminarAccionesAntiguas(@Param("limite") Instant limite);

    /**
     * ✅ NUEVO: Cuenta cuántas acciones antiguas se eliminarán.
     * Útil para saber el impacto antes de borrar.
     */
    @Query("""
            SELECT COUNT(h) FROM GrupoHistorial h
            WHERE h.fecha < :limite
            """)
    long countAccionesAntiguas(@Param("limite") Instant limite);

    // ============================================================
    // VALIDACIONES
    // ============================================================

    /**
     * ✅ NUEVO: Verifica si existe alguna acción de un tipo específico en un grupo.
     */
    @Query("""
            SELECT COUNT(h) > 0 FROM GrupoHistorial h
            WHERE h.grupo.id = :grupoId
              AND h.accion = :accion
            """)
    boolean existeAccion(
            @Param("grupoId") Long grupoId,
            @Param("accion") String accion
    );
}