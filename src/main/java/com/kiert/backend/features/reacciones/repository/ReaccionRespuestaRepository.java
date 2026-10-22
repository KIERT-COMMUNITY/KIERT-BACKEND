// src/main/java/com/kiert/backend/repository/ReaccionRespuestaRepository.java
package com.kiert.backend.features.reacciones.repository;

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

    @Query("""
            SELECT r.tipo, COUNT(r)
            FROM ReaccionRespuesta r
            WHERE r.respuesta.id = :respuestaId
            GROUP BY r.tipo
            """)
    List<Object[]> countReaccionesByRespuesta(@Param("respuestaId") Long respuestaId);

    long countByRespuestaId(Long respuestaId);

    // ============================================================
    // CONTAR REACCIONES POR MÚLTIPLES RESPUESTAS
    // ============================================================

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
    // VERIFICAR REACCIONES (bulk)
    // ============================================================

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

    @Modifying
    @Query("DELETE FROM ReaccionRespuesta r WHERE r.respuesta.id = :respuestaId")
    int deleteByRespuestaId(@Param("respuestaId") Long respuestaId);

    @Modifying
    @Query("DELETE FROM ReaccionRespuesta r WHERE r.respuesta.id IN :respuestaIds")
    int deleteByRespuestaIdIn(@Param("respuestaIds") List<Long> respuestaIds);

    // ============================================================
    // TOP RESPUESTAS REACCIONADAS
    // ============================================================

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

    @Query("""
            SELECT DISTINCT r.tipo FROM ReaccionRespuesta r
            ORDER BY r.tipo ASC
            """)
    List<String> findDistinctTipos();

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    @Query("SELECT COUNT(r) FROM ReaccionRespuesta r")
    long countTotal();

    @Query("""
            SELECT r.tipo, COUNT(r)
            FROM ReaccionRespuesta r
            GROUP BY r.tipo
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> contarGlobalPorTipo();

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
    // LIMPIEZA
    // ============================================================

    @Modifying
    @Query("""
            DELETE FROM ReaccionRespuesta r
            WHERE r.respuesta IS NULL
            """)
    int eliminarHuerfanas();

    @Modifying
    @Query("""
            DELETE FROM ReaccionRespuesta r
            WHERE r.respuesta.eliminado = true
            """)
    int eliminarDeRespuestasEliminadas();

    // ============================================================
    // SEGURIDAD: Validar autoría de la reacción
    // ============================================================

    /**
     * NUEVO: Verifica si un usuario es el autor de la reacción.
     * Se usa para impedir que un usuario elimine reacciones ajenas.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM ReaccionRespuesta r
            WHERE r.id = :reaccionId
              AND r.usuario.id = :usuarioId
            """)
    boolean esReaccionDelUsuario(
            @Param("reaccionId") Long reaccionId,
            @Param("usuarioId") Long usuarioId
    );
}