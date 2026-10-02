// src/main/java/com/kiert/backend/repository/ReaccionRespuestaRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.ReaccionRespuesta;
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
public interface ReaccionRespuestaRepository extends JpaRepository<ReaccionRespuesta, Long> {

    // ============================================================
    // BUSCAR REACCIÓN ESPECÍFICA
    // ============================================================

    /**
     * Busca la reacción de un usuario a una respuesta.
     */
    @Query("""
            SELECT r FROM ReaccionRespuesta r
            LEFT JOIN FETCH r.usuario
            WHERE r.usuario.id = :usuarioId
              AND r.respuesta.id = :respuestaId
            """)
    Optional<ReaccionRespuesta> findByUsuarioIdAndRespuestaId(
            @Param("usuarioId") Long usuarioId,
            @Param("respuestaId") Long respuestaId
    );

    // ============================================================
    // CONTAR REACCIONES (1 respuesta)
    // ============================================================

    /**
     * Cuenta reacciones agrupadas por tipo en una respuesta.
     * Devuelve [tipo, count].
     */
    @Query("""
            SELECT r.tipo, COUNT(r)
            FROM ReaccionRespuesta r
            WHERE r.respuesta.id = :respuestaId
            GROUP BY r.tipo
            """)
    List<Object[]> countReaccionesByRespuesta(@Param("respuestaId") Long respuestaId);

    long countByRespuestaId(Long respuestaId);

    // ============================================================
    // 🔥 OPTIMIZACIÓN CRÍTICA: CONTAR REACCIONES POR MÚLTIPLES RESPUESTAS
    // ============================================================

    /**
     * ✅ NUEVO: Cuenta reacciones de VARIAS respuestas en 1 query.
     * Devuelve [respuestaId, tipo, count].
     *
     * Elimina el N+1 al listar un comentario con todas sus respuestas.
     * Para 20 respuestas con reacciones: de 20 queries a 1.
     */
    @Query("""
            SELECT r.respuesta.id, r.tipo, COUNT(r)
            FROM ReaccionRespuesta r
            WHERE r.respuesta.id IN :respuestaIds
            GROUP BY r.respuesta.id, r.tipo
            """)
    List<Object[]> contarReaccionesPorRespuestas(
            @Param("respuestaIds") List<Long> respuestaIds
    );

    // ============================================================
    // REACCIONES DEL USUARIO
    // ============================================================

    /**
     * ✅ NUEVO: Reacciones de un usuario a respuestas (paginado).
     */
    @Query("""
            SELECT r FROM ReaccionRespuesta r
            LEFT JOIN FETCH r.respuesta resp
            WHERE r.usuario.id = :usuarioId
              AND resp.eliminado = false
            ORDER BY r.fechaCreacion DESC
            """)
    Page<ReaccionRespuesta> findReaccionesDeUsuario(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    // ============================================================
    // VERIFICAR REACCIONES (bulk, para el listado)
    // ============================================================

    /**
     * ✅ NUEVO: Devuelve los IDs de respuestas donde el usuario reaccionó.
     * Útil para marcar "ya reaccioné" en un hilo de respuestas.
     */
    @Query("""
            SELECT r.respuesta.id FROM ReaccionRespuesta r
            WHERE r.usuario.id = :usuarioId
              AND r.respuesta.id IN :respuestaIds
            """)
    List<Long> findRespuestasDondeReaccione(
            @Param("usuarioId") Long usuarioId,
            @Param("respuestaIds") List<Long> respuestaIds
    );

    // ============================================================
    // ELIMINAR REACCIONES
    // ============================================================

    /**
     * Elimina la reacción de un usuario a una respuesta.
     * ✅ MEJORA: usa @Modifying para evitar el SELECT + DELETE.
     */
    @Modifying
    @Query("""
            DELETE FROM ReaccionRespuesta r
            WHERE r.usuario.id = :usuarioId
              AND r.respuesta.id = :respuestaId
            """)
    int deleteByUsuarioIdAndRespuestaId(
            @Param("usuarioId") Long usuarioId,
            @Param("respuestaId") Long respuestaId
    );

    /**
     * ✅ NUEVO: Elimina TODAS las reacciones de una respuesta (bulk).
     * Útil cuando se elimina una respuesta.
     */
    @Modifying
    @Query("DELETE FROM ReaccionRespuesta r WHERE r.respuesta.id = :respuestaId")
    int deleteByRespuestaId(@Param("respuestaId") Long respuestaId);

    /**
     * ✅ NUEVO: Elimina TODAS las reacciones de múltiples respuestas en 1 query.
     * Útil cuando se elimina un comentario con todas sus respuestas.
     */
    @Modifying
    @Query("DELETE FROM ReaccionRespuesta r WHERE r.respuesta.id IN :respuestaIds")
    int deleteByRespuestaIdIn(@Param("respuestaIds") List<Long> respuestaIds);

    // ============================================================
    // TOP RESPUESTAS REACCIONADAS
    // ============================================================

    /**
     * ✅ NUEVO: Top N respuestas con más reacciones.
     * Devuelve [respuestaId, totalReacciones].
     */
    @Query("""
            SELECT r.respuesta.id, COUNT(r) as total
            FROM ReaccionRespuesta r
            GROUP BY r.respuesta.id
            ORDER BY total DESC
            """)
    List<Object[]> topRespuestasMasReaccionadas(Pageable pageable);

    // ============================================================
    // TIPOS DE REACCIÓN DISPONIBLES
    // ============================================================

    /**
     * ✅ NUEVO: Tipos de reacción distintos que existen.
     */
    @Query("""
            SELECT DISTINCT r.tipo FROM ReaccionRespuesta r
            ORDER BY r.tipo ASC
            """)
    List<String> findDistinctTipos();

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * ✅ NUEVO: Total de reacciones a respuestas en el sistema.
     */
    @Query("SELECT COUNT(r) FROM ReaccionRespuesta r")
    long countTotal();

    /**
     * ✅ NUEVO: Cuenta reacciones agrupadas por tipo (global).
     * Devuelve [tipo, count].
     */
    @Query("""
            SELECT r.tipo, COUNT(r)
            FROM ReaccionRespuesta r
            GROUP BY r.tipo
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> contarGlobalPorTipo();

    /**
     * ✅ NUEVO: Reacciones creadas en un rango de fechas.
     */
    @Query("""
            SELECT COUNT(r) FROM ReaccionRespuesta r
            WHERE r.fechaCreacion >= :desde
              AND r.fechaCreacion < :hasta
            """)
    long countEnRango(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * ✅ NUEVO: Elimina reacciones huérfanas
     * (respuesta eliminada o inexistente).
     */
    @Modifying
    @Query("""
            DELETE FROM ReaccionRespuesta r
            WHERE r.respuesta IS NULL
            """)
    int eliminarHuerfanas();

    /**
     * ✅ NUEVO: Elimina reacciones de respuestas que fueron soft-deleted.
     */
    @Modifying
    @Query("""
            DELETE FROM ReaccionRespuesta r
            WHERE r.respuesta.eliminado = true
            """)
    int eliminarDeRespuestasEliminadas();
}